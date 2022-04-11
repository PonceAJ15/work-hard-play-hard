//SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;
import "./MerkleWorkToken.sol";
import "./LinearWorkToken.sol";

contract W2HPToken is ERC20, MerkleWorkToken, LinearWorkToken {
    constructor() ERC20("Work Hard Play Hard Protocol reward token","W2HP") {}
    function decimals() public pure override returns(uint8) {return 7;}
    uint256 constant _dailyMintLimit = 50*6*24*(8**10);
    function _getMintLimit() public override pure returns(uint256) {
        return _dailyMintLimit;
    }
}