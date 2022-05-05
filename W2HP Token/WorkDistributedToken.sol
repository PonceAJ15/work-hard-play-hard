//SPDX-License-Identifier: MIT
//This code is provided as is with no warranty.
pragma solidity ^0.8.0;

import "./IBEP20.sol";

//Private, secure, feeless, censorship resistant and highly liquid micropayments processing.
//Private             : Proof of work nonces have no data associated with the identity of the miner.
//Secure              : Hash protected secrets prevent miners from interfering with payment integrity.
//Feeless             : The cost complexity of processing n micropayments is O(1).
//Censorship resistant: Inherits the censorship resistance of the host blockchain.
//Liquid              : This code targets the Ethereum Virtual Machine, giving it access to billions of dollars
//                      worth of liquidity that already exists on large networks like Ethereum!
//Any funds that are sent to this contract are distributed equally to all token holders!
contract WorkDistributedToken is IBEP20 {
    uint256 public constant MINT_LIMIT = 50*6*24*10**8;
    uint64 internal constant SEED_BIT = 2**63;
    uint16 internal constant TRANSACTION_GAS = 11600;
    uint256 internal constant WORK_DAY = 1 days;
    uint256 internal constant MAX_INT = type(uint256).max;
    uint8 internal constant NO_REENTRY = 1;
    uint8 internal constant REENTRY = 2;
    //April 20th, 00h:00m:00s 2022 GMT-0500 Central Daylight Time
    uint256 internal constant GENESIS = 1650430800;

    //Accounts take up 32 bytes of space in storage, the minimum amount of space allocatable on the Ethereum state trie.
    struct Account {
        uint96        work;//The amount of work a user has done since the day lastActive.
        uint64        seed;//The public seed for the users proof of work problems.
        uint56     balance;//The amount of tokens that a user owns.
        uint24  lastActive;//This is the most recent day that the user was active on.
        uint16    checkNum;//This value is incremented for each submitted Proof of Work to prevent replay attacks.
    }

    address internal _owner;
    uint8 internal _reentry = NO_REENTRY;
    mapping(address => Account) public _balances;
    mapping(address => mapping(address => uint256)) public _allowances;
    mapping(uint24 => uint256) public _totalWorks;
    mapping(address => bytes32) public _secrets;

    //Step 2: The server owner claims the value of their work.
    function workClaim(bytes32 nonce, bytes32 solution) external {
        //Copy the users account state to memory to avoid storage related gas fees.
        Account memory worker = _balances[msg.sender];
        //Estimate the hashes needed to get this nonce, both the client and the server are aware of this value.
        uint256 hash = uint256(keccak256(abi.encodePacked(msg.sender,uint32(worker.checkNum),worker.seed,nonce)));
        uint256 value = MAX_INT/hash;
        //If the seed is not 0, then the user must have a secret.
        if(worker.seed != 0) {
            //If the user provides a valid solution to the secret, perform multiplication of work value.
            while(keccak256(abi.encode(solution)) == _secrets[msg.sender]) {
                //If the users seed has yet to be generated, attempt to generate the seed.
                if (worker.seed < SEED_BIT) {
                    uint64 noise = uint64(uint256(blockhash(worker.seed)));
                    //If the commited blockhash is inaccessible, skip the multiplication of work value.
                    if(noise == 0) break;
                    //Generate the public seed for proof of work.
                    worker.seed = noise | SEED_BIT;
                    //Recalculate the initial work estimate with using the newly calculated public seed.
                    hash = uint256(keccak256(abi.encodePacked(msg.sender,uint32(worker.checkNum),worker.seed,nonce)));
                    value = MAX_INT/hash;
                }
                //Use the solution bytes to generate a private seed for proof of work multiplication
                uint64 privateSeed = uint64(worker.seed^uint256(solution));
                //Estimate the amount of nonces needed to get this secret value, then multiply the work estimate.
                hash = uint256(keccak256(abi.encodePacked(msg.sender,uint32(worker.checkNum),privateSeed,hash)));
                value *= MAX_INT/hash;
                //Store the solution as the next secret. This allows for secret value chaining.
                _secrets[msg.sender] = solution;
                //Generate a new seed if the users seed is more than 2 blocks old.
                uint64 newSeed = uint64(uint256(blockhash(block.number-1))) | SEED_BIT;
                uint64 prevSeed = uint64(uint256(blockhash(block.number-2))) | SEED_BIT;
                if(newSeed != worker.seed && prevSeed != worker.seed)
                    worker.seed = newSeed;
                break;
            }
            //If by this point a seed has yet to be generated, commit to use the next blockhash.
            if(worker.seed < SEED_BIT)
                worker.seed = uint64(uint256(block.number+1));
        }
        //Now that the work value has calculated, the checknum can be incremented.
        worker.checkNum++;
        uint24 today = uint24((block.timestamp - GENESIS)/WORK_DAY);
        //If work has already been submitted today, then just add on the new work.
        if(worker.lastActive == today) worker.work += uint96(value);
        //If the user has worked on a past day, give them tokens for their past work.
        else {
            //Tokens are distributed based on the amount of work done per day relative to all work done that day.
            uint56 amount = uint56((MINT_LIMIT*worker.work)/_totalWorks[worker.lastActive]);
            if(amount > 0) {
                _totalSupply += amount;
                worker.balance += uint56(amount);
                emit Transfer(address(0), msg.sender, amount);
            }
            //Don't forget to update their lastActive counter to today
            worker.lastActive = today;
            worker.work = uint96(value);
        }
        //copy the users new account state from memory to storage.
        _balances[msg.sender] = worker;
        //update the total work for today.
        _totalWorks[today] += value;
    }
    //Step 1.5: The server invokes the contract to generate a seed value for their account.
    function storeBlockhash() external {
        //The contract attempts to retrive the blockhash it previously committed to using.
        uint64 noise = uint64(uint256(blockhash(_balances[msg.sender].seed)));
        //If the committed blockhash can't be accessed, the transaction will not go through. 
        require(noise != 0, "WDT: Seed can not be generated.");
        //store the blockhash for later use.
        _balances[msg.sender].seed = noise | SEED_BIT;
    }
    //Step 1: The server generates 32 secret bytes and passes the hash of the bytes this function.
    function commitSecret(bytes32 newSecret) external {
        //The contract commits to use the hash of the next block as a noise source for this account.
        _balances[msg.sender].seed = uint64(block.number+1);
        //The hash of the secret is committed to the blockchain.
        _secrets[msg.sender] = newSecret;
    }
    //Funds sent to the contract are released by burning tokens.
    receive() external payable {}
    //Special burn functions
    function burn(uint256 amount, address[] calldata tokens) external {
        _burn(msg.sender, amount);
        _tokenRebate(msg.sender, amount, tokens);
    }
    function burnFrom(address account, uint256 amount, address[] calldata tokens) external {
        _spendAllowance(account, msg.sender, amount);
        _burn(msg.sender, amount);
        _tokenRebate(msg.sender, amount, tokens);
    }
    function _tokenRebate(address account, uint256 amount, address[] calldata tokens) internal {
        require(_reentry == NO_REENTRY, "https://www.youtube.com/watch?v=93Fyv4XAr-A");
        _reentry = REENTRY;
        address self = address(this);
        for(uint i = 0; i < tokens.length; i++) {
            IBEP20 token = IBEP20(tokens[i]);
            token.transfer(account, (token.balanceOf(self)*amount)/(_totalSupply+amount));
        }
        _reentry = NO_REENTRY;
    }
    //Function necessary for compliance with IBEP20 standard.
    function getOwner() external view override returns(address) {
        return _owner;
    }
    //Functions from OpenZepplin's ERC20Burnable extension.
    function burn(uint256 amount) external {
        _burn(msg.sender, amount);
    }
    function burnFrom(address account, uint256 amount) external {
        _spendAllowance(account, msg.sender, amount);
        _burn(account, amount);
    }
    //Functions from the OpenZepplin ERC20 standard implementation, modified for the needs of this contract.
    uint256 private _totalSupply;
    string private _name;
    string private _symbol;
    constructor(string memory name_, string memory symbol_) {
        //ERC20/BEP20 metadata
        _name = name_;
        _symbol = symbol_;
        //make the deployer the owner and give him 1 day worth of tokens
        _owner = msg.sender;
        _totalSupply += MINT_LIMIT;
        _balances[msg.sender].balance = uint56(MINT_LIMIT);
        emit Transfer(address(0), msg.sender, MINT_LIMIT);
        //Every account will try to claim tokens from day 0 initially. There is no amount
        //of work which will allow a user to claim ANY tokens when totalWork is 2^256.
        _totalWorks[0] = MAX_INT;
    }
    
    function name() public view override returns (string memory)
        {return _name;}
    function symbol() public view override returns (string memory)
        {return _symbol;}
    function decimals() public pure override returns (uint8)
        {return 8;}
    function totalSupply() public view override returns (uint256)
        {return _totalSupply;}
    function balanceOf(address account) public view override returns (uint256)
        {return _balances[account].balance;}
    function transfer(address to, uint256 amount) public override returns (bool)
        {_transfer(msg.sender, to, amount);return true;}
    function allowance(address owner, address spender) public view override returns (uint256)
        {return _allowances[owner][spender];}
    function approve(address spender, uint256 amount) public override returns (bool)
        {_approve(msg.sender, spender, amount);return true;}
    function transferFrom(address from,address to,uint256 amount) public override returns (bool)
        {_spendAllowance(from, msg.sender, amount);_transfer(from, to, amount);return true;}
    function increaseAllowance(address spender, uint256 addedValue) public returns (bool)
        {_approve(msg.sender, spender, allowance(msg.sender, spender) + addedValue);return true;}
    function decreaseAllowance(address spender, uint256 subtractedValue) public returns (bool) {
        uint256 currentAllowance = allowance(msg.sender, spender);
        require(currentAllowance >= subtractedValue, "ERC20: decreased allowance below zero");
        unchecked {
            _approve(msg.sender, spender, currentAllowance - subtractedValue);
        }
        return true;
    }
    function _transfer(address from,address to,uint256 amount) internal {
        require(from != address(0), "ERC20: transfer from the zero address");
        require(to != address(0), "ERC20: transfer to the zero address");
        uint256 fromBalance = _balances[from].balance;
        require(fromBalance >= amount, "ERC20: transfer amount exceeds balance");
        unchecked {
            _balances[from].balance = uint56(fromBalance - amount);
        }
        _balances[to].balance += uint56(amount);
        emit Transfer(from, to, amount);
    }
    function _burn(address account, uint256 amount) internal {
        require(account != address(0), "ERC20: burn from the zero address");
        uint256 accountBalance = _balances[account].balance;
        require(accountBalance >= amount, "ERC20: burn amount exceeds balance");
        uint256 rebate = (amount*address(this).balance)/totalSupply();
        if(rebate > TRANSACTION_GAS * tx.gasprice) payable(account).transfer(rebate);
        unchecked {
            _totalSupply -= amount;
            _balances[account].balance = uint56(accountBalance - amount);
        }
        emit Transfer(account, address(0), amount);
    }
    function _approve(address owner,address spender,uint256 amount) internal {
        require(owner != address(0), "ERC20: approve from the zero address");
        require(spender != address(0), "ERC20: approve to the zero address");
        _allowances[owner][spender] = amount;
        emit Approval(owner, spender, amount);
    }
    function _spendAllowance(address owner,address spender,uint256 amount) internal {
        uint256 currentAllowance = allowance(owner, spender);
        if (currentAllowance != type(uint256).max) {
            require(currentAllowance >= amount, "ERC20: insufficient allowance");
            unchecked {
                _approve(owner, spender, currentAllowance - amount);
            }
        }
    }
}