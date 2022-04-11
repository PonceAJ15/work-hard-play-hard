//SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;
import "./ERC20.sol";

abstract contract WorkToken is ERC20 {
    mapping(uint32 => uint256) public _totalWorks;
    mapping(address => MinerState) public _workers;

    function redeemTokens() public {
        _mint(_msgSender(), _workers[_msgSender()].redeemable);
        _workers[_msgSender()].redeemable = 0;
    }

    //mutable information about work done by account
    struct MinerState { //1 memory slot => 28 bytes
        uint112       work; //14 bytes|Enough to store 5 decillion hashes of work
        uint64  redeemable; //8  bytes|Enough to store 70,000 years worth of token supply
        uint32  lastActive; //4  bytes|
        uint32    checkNum; //4  bytes|Enough to submit 4 billion proof of works.
        uint32   holdCheck; //4  bytes|Check num of current merkle proof.
    }

    function _getMintLimit() public virtual pure returns(uint256);

    function _addWork(address to, uint80 work, bool updateCheck) internal {
        MinerState storage worker = _workers[to];
        if(updateCheck) {worker.checkNum++;}
        if(worker.lastActive == today()) {
            worker.work += work;
        }else if(worker.lastActive == 0) {
            worker.lastActive = today();
            worker.work = work;
        }else{
            worker.redeemable += uint48((_getMintLimit()*worker.work)/_totalWorks[worker.lastActive]);
            worker.lastActive = today();
            worker.work = work;
        }
        _totalWorks[today()] += uint256(work);
    }

    function today() internal view returns (uint32) {
        //TODO: implement day calculation
        return uint32(block.timestamp/(1 days));
    }

    function hashValue(uint256 hash) internal pure returns(uint64){
        //TODO: implement hashValue calculation
        return uint64(type(uint256).max - hash);
    }

}