//SPDX-License-Identifier: MIT
//This code is provided as is with no warranty.
pragma solidity ^0.8.0;

import "./IERC20Metadata.sol";

contract W2HPToken is IERC20Metadata {
    uint56 constant ONE_TOKEN = 8**10;
    uint80 constant HEXA_HASH = 2**16;
    uint256 constant MINT_LIMIT = 50*6*24*ONE_TOKEN;    
    uint256 constant GENESIS = 1650430800;

    struct Account { //1 memory slot => 32 bytes
        uint80        work; //10 bytes|Enough to store 5 decillion hashes of work
        int64         seed; //8  bytes|<0?num of noise source block:seed
        uint56     balance; //7  bytes|Any account can own 247 years max of token supply.
        uint32    checkNum; //4  bytes|Enough to submit 4 billion proof of works.
        uint24  lastActive; //3  bytes|Mining will work as intended for 45,964 years.
    }

    mapping(address => bytes32) internal _secrets;
    mapping(uint24 => uint256) internal _totalWorks;
    mapping(address => Account) private _balances;
    mapping(address => mapping(address => uint256)) private _allowances;

    //submit proof of work to the blockchain.
    function workClaim(bytes32 nonce, bytes32 solution) external returns (bool) {
        Account storage worker = _balances[msg.sender];
        uint32 check = worker.checkNum++;
        int64 seed = worker.seed;
        bytes32 hash = keccak256(abi.encodePacked(msg.sender,check,seed,nonce));
        uint256 value = type(uint256).max/(uint256(hash)+1);
        do  {
            if(seed == 0) {continue;}
            worker.seed = 0;
            bytes32 secret = _secrets[msg.sender];
            if(keccak256(abi.encode(solution)) == secret) {
                if (seed < 0) {
                    bytes32 noise = blockhash(uint64(seed*-1));
                    if(noise == 0) {continue;}
                    seed = int64(int256(uint256(keccak256(abi.encode(noise^secret)))));
                    if(seed < 0) {
                        seed *= -1;
                    }
                }
                bytes8 fun = bytes8(uint64(seed))^bytes8(solution);
                hash = keccak256(abi.encodePacked(msg.sender,check,fun,hash));
                //Only those who know the solution can calculate the true value
                //of any given proof of work nonce. This makes attacks against 
                //mining collectives ineffective and allows the EVM to effectively
                //verify `mul` nonces worth of work from 1 sample nonce. 
                uint256 mul = type(uint256).max/(uint256(hash)+1);
                if(mul > 255) {
                    value *= mul;
                }
            }
        } while(false);
        uint80 work = uint80(value/HEXA_HASH);
        if(worker.lastActive == today()) {
            worker.work += work;
        }else if(worker.lastActive == 0) {
            worker.lastActive = today();
            worker.work += work;
        }else {
            uint56 amount = uint56((MINT_LIMIT*worker.work)/_totalWorks[worker.lastActive]);
            _totalSupply += amount;
            worker.balance += amount;
            emit Transfer(address(0), msg.sender, amount);
            worker.lastActive = today();
            worker.work = work;
        }
        _totalWorks[today()] += work;
        return true;
    }
    function tellSecret(bytes32 newSecret) external {
        Account storage worker = _balances[msg.sender];
        require(worker.seed == 0, "W2HP: Account already has a secret.");
        worker.seed = int64(int256(block.number+1))*-1;
        _secrets[msg.sender] = newSecret;
    }
    function generateSeed() external {
        Account storage worker = _balances[msg.sender];
        bytes32 noise = blockhash(uint64(worker.seed*-1));
        require(worker.seed < 0 && noise != 0, "W2HP: Seed can not be generated.");
        int64 newSeed = int64(int256(uint256(keccak256(abi.encode(noise^_secrets[msg.sender])))));
        if(newSeed < 0) {
            newSeed *= -1;
        }
        worker.seed = newSeed;
    }
    function today() internal view returns (uint24) {
        return uint24((block.timestamp - GENESIS)/(1 days));
    }

    //Everything below this comment is code modified from the OpenZepplin ERC20 standard.
    uint256 private _totalSupply;
    string private _name;
    string private _symbol;
    constructor(string memory name_, string memory symbol_) {
        _name = name_;
        _symbol = symbol_;
    }
    
    function name() public view override returns (string memory) {return _name;}
    function symbol() public view override returns (string memory) {return _symbol;}
    function decimals() public pure override returns (uint8) {return 7;}
    function totalSupply() public view override returns (uint256) {return _totalSupply;}
    function balanceOf(address account) public view override returns (uint256) {return _balances[account].balance;}

    function transfer(address to, uint256 amount) public override returns (bool) {
        _transfer(msg.sender, to, amount);
        return true;
    }
    function allowance(address owner, address spender) public view override returns (uint256) {
        return _allowances[owner][spender];
    }
    function approve(address spender, uint256 amount) public override returns (bool) {
        _approve(msg.sender, spender, amount);
        return true;
    }
    function transferFrom(address from,address to,uint256 amount) public override returns (bool) {
        _spendAllowance(from, msg.sender, amount);
        _transfer(from, to, amount);
        return true;
    }
    function increaseAllowance(address spender, uint256 addedValue) public returns (bool) {
        _approve(msg.sender, spender, allowance(msg.sender, spender) + addedValue);
        return true;
    }
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
    
    function burn(uint256 amount) public {_burn(msg.sender, amount);}
    function _burn(address account, uint256 amount) internal {
        require(account != address(0), "ERC20: burn from the zero address");
        uint256 accountBalance = _balances[account].balance;
        require(accountBalance >= amount, "ERC20: burn amount exceeds balance");
        unchecked {
            _balances[account].balance = uint56(accountBalance - amount);
        }
        _totalSupply -= amount;
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