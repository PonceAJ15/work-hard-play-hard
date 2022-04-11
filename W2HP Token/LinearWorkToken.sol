//SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;
import "./WorkToken.sol";

abstract contract LinearWorkToken is WorkToken {
    //commits all proof of works in array to balance
    function multipleWorkClaim(uint64[] calldata nonce) public returns (bool) {
        uint32 check = _workers[_msgSender()].checkNum;
        bytes24 head = bytes24(abi.encodePacked(_msgSender(),check));
        uint64 total = 0;
        for(uint32 i = 0; i < nonce.length; i++) {
            total+=hashValue(uint256(keccak256(abi.encodePacked(head,i,nonce[i]))));
        }
        _addWork(_msgSender(), total, true);
        return true;
    }

    //commits 1 proof of work to balance
    function workClaim(uint32 nonce) public returns (bool) {
        uint32 check = _workers[_msgSender()].checkNum;
        uint256 hash = uint256(keccak256(abi.encodePacked(_msgSender(),check,nonce)));
        _addWork(_msgSender(), hashValue(hash), true);
        return true;
    }

}