//SPDX-License-Identifier: MIT
//This code is provided as is with no warranty.
pragma solidity ^0.8.0;

import "./IERC20.sol";
import "./ERC20.sol";

//Implementation of a liquidity pool based Automated Market Maker for ERC20 tokens.
//This is for learning purposes only, better Automated Market Makers already exist.
contract NaiveAMM is ERC20 {
    uint256 constant BIG_ONE = 10**18;
    uint256 internal _tradeableTokens;
    uint256 internal _tradeableCoins;
    address internal _token;

    constructor(address token_, string memory name_, string memory symbol_) ERC20(name_, symbol_) {
        _token = token_;
    }

    //To get n% of the tradeable eth supply, a user must give n% of the tradeable token
    //supply in exchange.
    function getCoins(uint256 amount) external {
        uint256 coinAmount = (_tradeableCoins*amount)/_tradeableTokens;
        _tradeableCoins -= coinAmount;
        _tradeableTokens += amount;
        IERC20(_token).transferFrom(msg.sender,address(this),amount);
        //.3% trading Fee is given to the liquidity providers as a payment for risk incurred.
        payable(msg.sender).transfer((coinAmount*997)/1000);
    }
    //To get n% of the tradeable token supply, a user must give n% of the tradeable eth
    //supply in exchange.
    function getTokens() external payable {
        uint256 tokenAmount = (_tradeableTokens*msg.value)/_tradeableCoins;
        _tradeableTokens -= tokenAmount;
        _tradeableCoins += msg.value;
        //.3% trading Fee is given to the liquidity providers as a payment for risk incurred.
        IERC20(_token).transfer(msg.sender,(tokenAmount*997)/1000);
    }

    //Step 3: Liquidity provider cashes out their share of pool.
    function withdrawLiquidity(uint256 amount) external {
        IERC20 token = IERC20(_token);
        uint256 percent = (amount*BIG_ONE)/totalSupply();
        _burn(msg.sender, amount);
        _tradeableTokens -= (_tradeableTokens*percent)/BIG_ONE;
        _tradeableCoins -= (_tradeableCoins*percent)/BIG_ONE;
        payable(msg.sender).transfer((address(this).balance*percent)/BIG_ONE);
        token.transfer(msg.sender, (token.balanceOf(address(this))*percent)/BIG_ONE);
    }
    //Step 2: Liquidity provider deposits ETH and tokens in exchange for a share of the pool.
    function depositLiquidity(uint256 expectedRatio, uint256 tolerance) external payable {
        uint256 totalTokens = IERC20(_token).balanceOf(address(this));
        uint256 totalCoins = address(this).balance - msg.value;
        uint256 ratio = (totalCoins*BIG_ONE)/totalTokens;
        uint256 error;
        if(ratio < expectedRatio) {
            error = expectedRatio - ratio;
        }else {
            error -= expectedRatio;
        }
        require(error > tolerance, "NaiveAMM: Token price out of specified tolerance range");
        uint256 tokens = (msg.value*ratio)/BIG_ONE;
        IERC20(_token).transferFrom(msg.sender,address(this),tokens);
        _mint(msg.sender,(msg.value*totalSupply())/(totalCoins));
        _tradeableCoins += msg.value;
        _tradeableTokens += tokens;
    }

    //Step 1: liquidity provider approves this contract to spend their tokens.

    //Step 0: developer deposts a small amount of liquidity start the pool.
    function initialLiquidity(uint256 tokenAmount) external payable {
        require(totalSupply() == 0, "NaiveAMM: Initial liquidity already provided.");
        IERC20(_token).transferFrom(msg.sender,address(this),tokenAmount);
        _tradeableTokens += tokenAmount;
        _tradeableCoins += msg.value;
        _mint(msg.sender, BIG_ONE);
    }
}