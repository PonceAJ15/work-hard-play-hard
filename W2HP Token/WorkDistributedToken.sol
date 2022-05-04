//SPDX-License-Identifier: MIT
//This code is provided as is with no warranty.
pragma solidity ^0.8.0;

import "./IERC20Metadata.sol";

//Private, secure, feeless, censorship resistant and highly liquid micropayments processing.
//Private             : Proof of work nonces have no data associated with the identity of the miner.
//Secure              : Hash protected secrets prevent miners from interfering with payment integrity.
//Feeless             : The cost complexity of processing micropayments is constant for any amount of transactions.
//Censorship resistant: Inherits the censorship resistance of the host blockchain.
//Liquid              : This code targets the Ethereum Virtual Machine, giving it access to billions of dollars
//                      worth of liquidity that already exists on large networks like Ethereum!
contract W2HPToken is IERC20Metadata {
    uint64 constant SEED_BIT = 2**63;
    uint80 constant HEXA_HASH = 2**16;
    uint256 constant MAX_INT = type(uint256).max;
    uint256 constant MINT_LIMIT = 50*6*24*10**8;
    uint256 constant GENESIS = 1650430800;

    //Accounts take up 32 bytes of space in storage, the minimum amount of space allocatable on the Ethereum state trie.
    struct Account {
        uint80        work;//The amount of work a user has done since the day lastActive.
        uint64        seed;//The public seed for the users proof of work problems.
        uint56     balance;//The amount of tokens that a user owns.
        uint32    checkNum;//This value is incremented for each submitted Proof of Work to prevent replay attacks.
        uint24  lastActive;//This is the most recent day that the user was active on.
    }

    mapping(address => bytes32) internal _secrets;
    mapping(address => Account) private _balances;
    mapping(address => mapping(address => uint256)) private _allowances;
    mapping(uint24 => uint256) internal _totalWorks;

    //Step 2: The server owner claims the value of their work.
    function workClaim(bytes32 nonce, bytes32 solution) external {
        //Copy the users account state to memory to avoid storage related gas fees.
        Account memory worker = _balances[msg.sender];
        //Estimate the hashes needed to get this nonce, both the client and the server are aware of this value.
        uint256 hash = uint256(keccak256(abi.encodePacked(msg.sender,worker.checkNum,worker.seed,nonce)));
        uint256 value = MAX_INT/hash;
        //If the users account has a seed value then it must also have a secret value.
        if(worker.seed != 0) {
            do {
                bytes32 secret = _secrets[msg.sender];
                //If the user provides a valid solution to the secret, perform multiplication of work value.
                if(keccak256(abi.encode(solution)) == secret) {
                    //If the users seed has yet to be generated, attempt to generate the seed.
                    if (worker.seed < SEED_BIT) {
                        bytes32 noise = blockhash(worker.seed);
                        //If the commited blockhash is inaccessible, skip the multiplication of work value.
                        if(noise == 0) {continue;}
                        //Generate the public seed for proof of work.
                        worker.seed = uint64(uint256(keccak256(abi.encode(noise^secret)))|SEED_BIT);
                        //Recalculate the initial work estimate with using the newly calculated public seed.
                        hash = uint256(keccak256(abi.encodePacked(msg.sender,worker.checkNum,worker.seed,nonce)));
                        value = MAX_INT/hash;
                    }
                    //Use the solution bytes to generate a private seed for proof of work multiplication
                    worker.seed = uint64(worker.seed^uint256(solution));
                    //Estimate the amount of nonces needed to get this secret value, then multiply the work estimate.
                    hash = uint256(keccak256(abi.encodePacked(msg.sender,worker.checkNum,worker.seed,hash)));
                    value *= MAX_INT/hash;
                    //Store the solution as the next secret. This allows for secret value chaining.
                    _secrets[msg.sender] = solution;
                }
            } while(false);
            //Commit to use the next block as a noise source. This allows for secret value chaining.
            worker.seed = uint64(block.number+1);
        }
        //Now that the work value has calculated, the checknum can be incremented.
        worker.checkNum++;
        uint80 work = uint80(value/HEXA_HASH);
        uint24 today = uint24((block.timestamp - GENESIS)/(1 days));
        //If work has already been submitted today, then just add on the new work.
        if(worker.lastActive == today) {
            worker.work += work;
        }
        //If the user has never submitted work before, update their lastActive counter to point to today.
        else if(worker.lastActive == 0) {
            worker.lastActive = today;
            worker.work += work;
        }
        //If the user has worked on a past day, give them the tokens they are entitled to.
        else {
            //Tokens are distributed based on the amount of work done per day relative to all work done that day.
            uint56 amount = uint56((MINT_LIMIT*worker.work)/_totalWorks[worker.lastActive]);
            worker.balance += amount;
            emit Transfer(address(0), msg.sender, amount);
            //Don't forget to update their lastActive counter to today
            worker.lastActive = today;
            worker.work = work;
        }
        //copy the users new account state from memory to storage. Using 1 storage operation is cheaper.
        _balances[msg.sender] = worker;
        //update the total work for today.
        _totalWorks[today] += work;
    }
    //Step 1.5: The server invokes the contract to generate a seed value for their account.
    function generateSeed() external {
        //The contract attempts to retrive the blockhash it previously committed to using.
        uint64 seed = _balances[msg.sender].seed;
        bytes32 noise = blockhash(seed);
        //If the committed blockhash can't be accessed, the transaction will not go through. 
        require(noise != 0, "W2HP: Seed can not be generated.");
        //Use the blockhash to generate a public seed for Proof of Work.
        seed = uint64(uint256(keccak256(abi.encode(noise))));
        //The seed bit verifies that the seed isn't a blocknumber.
        seed |= SEED_BIT;
        //Store the newly generated seed in the users account.
        _balances[msg.sender].seed = seed;
    }
    //Step 1: The server generates 32 secret bytes and passes the hash of the bytes this function.
    function commitSecret(bytes32 newSecret) external {
        //The contract commits to use the hash of the next block as a noise source for this account.
        _balances[msg.sender].seed = uint64(block.number+1);
        //The hash of the secret is stored on the blockchain for later use.
        _secrets[msg.sender] = newSecret;
    }
    //Claim earned tokens
    function manualTokenClaim(uint24 freeDay) external {
        Account memory worker = _balances[msg.sender];
        uint24 today = uint24((block.timestamp - GENESIS)/(1 days));
        //If no one works for a day, then anyone can claim all of that days tokens for free.
        if(freeDay < today && _totalWorks[freeDay] == 0) {
            _totalWorks[freeDay] = 1;
            worker.balance += uint56(MINT_LIMIT);
            emit Transfer(address(0), msg.sender, MINT_LIMIT);
        }
        if(worker.lastActive < today) {
            uint56 amount = uint56((worker.work*MINT_LIMIT)/_totalWorks[worker.lastActive]);
            worker.balance += amount;
            emit Transfer(address(0), msg.sender, amount);
            worker.work = 0;
        }
        _balances[msg.sender] = worker;
    }
    function forgetSeed() external {
        _balances[msg.sender].seed = 0;
    }
    //Functions from OpenZepplin's ERC20Burnable contract.
    function burn(uint256 amount) public {
        _burn(msg.sender, amount);
    }
    function burnFrom(address account, uint256 amount) public {
        _spendAllowance(account, msg.sender, amount);
        _burn(account, amount);
    }
    //Functions from the OpenZepplin ERC20 standard implementation, modified for the needs of this contract.
    uint256 private _burnt;
    string private _name;
    string private _symbol;
    constructor(string memory name_, string memory symbol_) {
        _name = name_;
        _symbol = symbol_;
    }
    
    function name() public view override returns (string memory)
        {return _name;}
    function symbol() public view override returns (string memory)
        {return _symbol;}
    function decimals() public pure override returns (uint8)
        {return 7;}
    function totalSupply() public view override returns (uint256)
        {return ((((block.timestamp - GENESIS)/(1 days))-1)*MINT_LIMIT)-_burnt;}
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
        unchecked {
            _burnt += amount;
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