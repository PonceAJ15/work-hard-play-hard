//SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;
import "./WorkToken.sol";

/*
* Gas fees may cost more than the value of tokens earned by mining 
* this altcoin if the hashValue() of proof of works is too low. This is
* unfortunate because lower end user machines can't be expected to solve
* hard proof of work problems, and they can't be trusted to try.
* 
* By using a merkle tree, we verify the integrity of 2^n proof of work
* using o(1) storage slots to hold mutable information about the tree,
* and all immutable tree information can be stored as a hash which points
* to the aforementioned storage slot.
*
* By using funds as collateral, the merkle tree can be verified by random
* sampling, where failing to provide the requested samples will result in the
* permanant loss of funds. If someone wants to submit a root with more work
* claimed than has been actually done, they are allowed to do so. But they
* will lose more than they earn by gambling over time. This is the house edge. 
*/
abstract contract MerkleWorkToken is WorkToken {
    //blockhash is assumed to be uniformally random.
    mapping(uint32 => bytes32) internal _noise;
    mapping(uint32 => uint32) internal _salvage;
    mapping(bytes32 => MerkleProofState) internal _merkleProofs;

    address public constant NO_SPONSER = address(0);
    uint48  public constant NO_COLLATERAL =       0;
    uint32  public constant NO_NOISE =            0;
    uint32  public constant NO_RESCUE =           0;
    bytes32 public constant EMPTY =      bytes32(0);
    uint32  public constant BLOCK_DELAY =         2;
    uint32  public constant READY =               0;
    uint16  public constant NO_PUNISHMENT =       1;
    uint8   public constant NO_HEIGHT =           0;
    uint8   public constant NO_SAMPLES =          0;
    uint    public constant COLLATERAL_SCALE = 2**8;


    //mutable information about the proof
    struct MerkleProofState {//1 storage slot => 32 bytes
        address    sponser; //20 bytes|
        uint48  collateral; //6  bytes|Scaled by 2^8, max 247 years worth of token supply
        uint32       noise; //4  bytes|Points to block.number+2
        uint8 sampleHeight; //1  bytes|255 maximum sample height
        uint8   sampleSize; //1  bytes|255 maximum samples
    }

    //memory only structure
    //container for proof verification variables
    struct MerkleHeader { //4 memory slots => 128 bytes
        bytes32       hash;
        bytes32       root;
        uint256 difficulty;
        address     worker;
        uint32       check;
        uint16      reward;
        uint16  punishment;
        uint8       height;
    }
    //memory only structure,
    //container for sample information
    struct MerkleSample { //1 memory slot => 2 bytes
        uint8 sampleHeight;
        uint8 sampleSize;
    }

    //Step 3: verify proof to release collateral and get work rewards.
    function merkleClaim(bytes32 root, uint256 difficulty, uint8 height, uint16 reward, address worker, uint32 check, bytes calldata proof) external {
        //set up environment
        MerkleHeader memory header = getHeader(root,difficulty,height,reward,worker,check);
        MerkleProofState storage proofState = _merkleProofs[header.hash];

        //verify work claim.
        bytes32 rand = noisePenalty(proofState,header);
        verifyClaim(header, proofState, proof, rand);

        //Distribute rewards
        uint256 work = (hashValue(difficulty)*2**height)/header.punishment;
        uint256 fee  = (work*reward)/10000;
        _addWork(proofState.sponser, uint80(fee), false);
        _mint(proofState.sponser, proofState.collateral*COLLATERAL_SCALE);
        _addWork(worker, uint80(work - fee), false);
        
        //Clean up state
        _workers[worker].checkNum = READY;
        delete _merkleProofs[header.hash];
    }

    /*
    function visitNodes(uint8 depth, uint8 remaining, uint64 branch, uint256 rand) puesdo_code returns(bool) {
        if(remaining == 1)
            return true;
        else if(remaining == 0)
            return true;
        if(remaining%2 == 0) {
            visitNodes(depth+1, remaining/2, branch*2, rand);
            visitNodes(depth+1, remaining/2, (branch*2)+1, rand);
        }else {
            if(rand%2 == 0) {
                visitNodes(depth+1, (remaining/2)+1, branch*2, rand/2);
                visitNodes(depth+1, remaining/2, (branch*2)+1, rand/2);
            }else {
                visitNodes(depth+1, remaining/2, branch*2, rand/2);
                visitNodes(depth+1, (remaining/2)+1, (branch*2)+1, rand/2);}}
    }
    The function visitNodes is a recursive algorithim for visiting n random nodes in a complete
    binary tree, using this function as reference, an iterative algorithm was developed that uses
    a virtual call stack to simulate the above recursive algorithm.
    */
    struct TreeBranch { //10 bytes of memory.
        uint64  parentBranch; //8 bytes
        uint8  leftRemaining; //1 bytes
        uint8 rightRemaining; //1 bytes
    }
    function verifyClaim(MerkleHeader memory header, MerkleProofState memory proofState, bytes calldata proof, bytes32 rand) internal view {
        bytes32 randMask = bytes32(uint256(1));
        uint8 subTrees = proofState.sampleSize;
        uint64 pp = 0;
        uint8 sp = 0;
        SubTree[] memory buffer = new SubTree[](proofState.sampleHeight);
        TreeBranch[] memory stack = new TreeBranch[](9);
        //initial step;
        if(subTrees%2 == 0) {
            stack[0] = TreeBranch(1,subTrees/2,subTrees/2);
        }else if((rand & randMask) == 0) {
            randMask <<= 1;
            stack[0] = TreeBranch(1,(subTrees/2)+1,subTrees/2);
        }else {
            randMask <<= 1;
            stack[0] = TreeBranch(1,subTrees/2,(subTrees/2)+1);
        }
        while(subTrees > 0) {
            TreeBranch memory item = stack[sp];
            //start by executing left branch
            if(item.leftRemaining > 1) {
                if(item.leftRemaining%2 == 0) {
                    stack[++sp] = TreeBranch(item.parentBranch*2,item.leftRemaining/2,item.leftRemaining/2);
                }else if((rand & randMask) == 0) {
                    randMask <<= 1;
                    stack[++sp] = TreeBranch(item.parentBranch*2,(item.leftRemaining/2)+1,item.leftRemaining/2);
                }else {
                    randMask <<= 1;
                    stack[++sp] = TreeBranch(item.parentBranch*2,item.leftRemaining/2,(item.leftRemaining/2)+1);
                }
            } else if(item.leftRemaining == 1) {
                rand = keccak256(abi.encodePacked(rand)); //shuffle randomness
                randMask = bytes32(uint256(1));
                uint256 start = header.height-sp;
                start = (uint256(rand)%start)*2**start + item.parentBranch*2**start;
                pp = verifySubTree(header,proofState,buffer,proof,pp,uint32(start));
                subTrees--;
                stack[sp].leftRemaining--;
            } else if(item.rightRemaining > 1) {
                if(item.rightRemaining%2 == 0) {
                    stack[++sp] = TreeBranch(item.parentBranch*2+1,item.rightRemaining/2,item.rightRemaining/2);
                }else if((rand & randMask) == 0) {
                    randMask <<= 1;
                    stack[++sp] = TreeBranch(item.parentBranch*2+1,(item.rightRemaining/2)+1,item.rightRemaining/2);
                }else {
                    randMask <<= 1;
                    stack[++sp] = TreeBranch(item.parentBranch*2+1,item.rightRemaining/2,(item.rightRemaining/2)+1);
                }
            } else if(item.rightRemaining == 1) {
                rand = keccak256(abi.encodePacked(rand)); //shuffle randomness
                randMask = bytes32(uint256(1));
                uint256 start = header.height-sp;
                start = (uint256(rand)%start)*2**start + item.parentBranch*2**start;
                pp = verifySubTree(header,proofState,buffer,proof,pp,uint32(start));
                subTrees--;
                stack[--sp].leftRemaining = 0;
            }else {
                sp--;
            }
        }
    }
    /*
    function recursiveSubTree(bytes calldata proof, bytes24 head, uint64 memPointer, uint8 height, uint32 start) internal view returns(bytes32) {
        if(height > 0) {
            height--;
            bytes32 leftHash = recursiveSubTree(proof, head, memPointer, height, start);
            bytes32 rightHash = recursiveSubTree(proof, head, uint64(memPointer+4*2**height), height, uint32(start+2**height));
            return pairHash(leftHash,rightHash);
        }else {
            bytes32 leftHash = keccak256(abi.encodePacked(head,start++,proof[memPointer:memPointer+4]));
            bytes32 rightHash = keccak256(abi.encodePacked(head,start,proof[memPointer+4:memPointer+8]));
            return pairHash(leftHash,rightHash);
        }
    }
    Reference recursive algorthim for implementation of iterative algorthim with virtual call stack
    */
    struct SubTree { //33 bytes of memory
        bytes32     hash; //32 bytes
        bool doRightHash; //1  bytes
    }
    function verifySubTree(MerkleHeader memory header, MerkleProofState memory proofState, SubTree[] memory stack, bytes calldata proof, uint64 memPointer, uint32 start) internal view returns(uint64) {
        bytes24 head = bytes24(abi.encodePacked(header.worker, _workers[header.worker].holdCheck));
        uint8 height = proofState.sampleHeight;
        uint parentHeight = header.height;
        uint8 sp = 0;
        stack[0] = SubTree(0,true);
        SubTree memory base = stack[0];
        while(base.doRightHash) {
            SubTree memory item = stack[sp];
            if(height == 1) { //near base level of tree, verify proof of work
                height++;
                item.hash = keccak256(abi.encodePacked(head,start,proof[memPointer:memPointer+4]));
                require(uint256(item.hash)<=header.difficulty, "W2HP: Merkle work less than claimed.");
                bytes32 tmp = keccak256(abi.encodePacked(head,start+1,proof[memPointer+4:memPointer+8]));
                require(uint256(tmp)<=header.difficulty, "W2HP: Merkle work less than claimed.");
                memPointer+=8;
                start+=2;
                item.hash = pairHash(item.hash,tmp);
                if(stack[--sp].hash == 0) { //bad practice, side effect in condition
                    stack[sp].hash = item.hash;
                }else {
                    stack[sp].doRightHash = false;
                    stack[sp].hash = pairHash(stack[sp].hash,item.hash);
                }
            }else if(item.hash == 0) { //left and right hash unknown
                height--;
                stack[++sp] = SubTree(0,true);
            }else if(item.doRightHash) { //right hash unknown
                height--;
                stack[++sp] = SubTree(0,true);
            }else if(stack[--sp].hash == 0) { //this hash is our parents left hash
                height++;
                stack[sp].hash = item.hash;
            }else { //this hash completes our parents hash.
                height++;
                stack[sp].doRightHash = false;
                stack[sp].hash = pairHash(stack[sp].hash,item.hash);
            }
        }
        start -= uint32(2**height);
        memPointer = (memPointer+32)/32;
        memPointer *= 32;
        bytes32 finalHash = base.hash;
        for(;height < parentHeight; height++) {
            if((start/(2**height))%2 == 0)
                finalHash = pairHash(bytes32(abi.encodePacked(proof[memPointer:memPointer+32])),finalHash);
            else
                finalHash = pairHash(finalHash,bytes32(abi.encodePacked(proof[memPointer:memPointer+32])));
            memPointer += 32;
        }
        //The whole entire point of ALL of this code is this line right here
        require(finalHash == header.root, "W2HP: Proof of work not in Merkle tree.");
        return memPointer;
    }
    function pairHash(bytes32 _a, bytes32 _b) internal pure returns(bytes32) {
      return keccak256(abi.encode(_a ^ _b));
    }
    function getProofHash(bytes32 root, uint256 difficulty, uint8 height, uint16 reward) internal pure returns(bytes32) {
        return keccak256(abi.encodePacked(root,difficulty,height,reward));
    }
    function getHeader(bytes32 root, uint256 difficulty, uint8 height, uint16 reward, address worker, uint32 check) internal pure returns (MerkleHeader memory) {
        MerkleHeader memory ret;
        ret.hash = getProofHash(root,difficulty,height,reward);
        ret.root = root;
        ret.difficulty = difficulty;
        ret.worker = worker;
        ret.check = check;
        ret.reward = reward;
        ret.punishment = NO_PUNISHMENT;
        ret.height = height;
        return ret;
    }
    /* We can generate randomness for the proving algorithm by requesting the blockhash of some future block, 
    * and then retriving the hash of the previously requested block once it has been committed to the blockchain.
    * This approach is good because it introduces no dependencies to external contracts, and it can be fairly cheap,
    * costing around ~200 gas to make work. However it is limited by the fact that the Ethereum Virtual Machine only
    * has access to the 256 most recent blockhashes. This function gets around that by only accepting blockhashes%256
    * as sources of noise, and punishing collateral providers severely for failing to verify their proofs in under 256
    * blocks after their noise request.
    */
    function noisePenalty(MerkleProofState storage proof, MerkleHeader memory header) internal returns(bytes32) {
        //Seed isn't lost yet (~200 gas).
        if(block.number-proof.noise <= 256) {
            return blockhash(proof.noise);
        }
        //Seed is lost,
        bytes32 noise = _noise[proof.noise];
        if(noise != EMPTY) {
            //but it was recorded (~2200 gas).
            return noise;
        }
        uint16 attempts;
        uint32 noiseBlock = _salvage[proof.noise];
        if(noiseBlock == NO_RESCUE) {
            //and no damage control has been done.
            attempts = uint16(((block.number - proof.noise)-1)/256);
            noiseBlock = proof.noise + (attempts*256);
            noise = blockhash(noiseBlock);
        }else {
            //but damage control has been done.
            attempts = uint16(((noiseBlock - proof.noise)-1)/256);
            noise = _noise[noiseBlock];
        }
        //lose half of collateral for each attempt
        proof.collateral /= uint48(2**attempts);
        header.punishment = attempts+1;
        return noise;
    }

    //Step 2: sponser proof with collateral.
    function sponserMerkleWorkClaim(bytes32 root, uint256 difficulty, uint8 height, uint16 reward, uint56 collateral) public returns (bool) {
        MerkleProofState storage proof = _merkleProofs[getProofHash(root, difficulty, height, reward)];
        uint64 size = uint64(2**height);
        uint256 value = hashValue(difficulty)*size;
        uint256 cValue = (collateral*_totalWorks[today()])/_getMintLimit();
        uint64 count = uint16((size*value)/(cValue*2));
        uint64 currentCallData = getCallData(height,proof.sampleHeight,proof.sampleSize);
        MerkleSample memory newSampling = distributeSamples(height, count);
        uint64 newCallData = getCallData(height, newSampling.sampleHeight, newSampling.sampleSize);
        require(newCallData < currentCallData, "W2HP: insufficient collateral");
        if(proof.sponser != NO_SPONSER) {
            _mint(proof.sponser, proof.collateral);
        }else {
            proof.noise = uint32(block.number + BLOCK_DELAY);
        }
        _burn(_msgSender(), collateral);
        proof.sponser = _msgSender();
        proof.collateral = uint48(collateral/COLLATERAL_SCALE);
        proof.sampleHeight = newSampling.sampleHeight;
        proof.sampleSize = newSampling.sampleSize;
        return true;
    }

    //Step 1: create mutable data structure that is mapped to by the hash of immutable proof data
    function initiateMerkleWorkClaim(uint64 maxCount, bytes32 root, uint256 difficulty, uint8 height, uint16 reward) public returns (bool) {
        require(_workers[_msgSender()].holdCheck == READY, "W2HP: Miner waiting on other merkle proof.");
        MinerState storage worker = _workers[_msgSender()];
        worker.holdCheck = worker.checkNum;
        worker.checkNum++;
        MerkleSample memory sample = distributeSamples(height,maxCount);
        _merkleProofs[getProofHash(root, difficulty, height, reward)] = MerkleProofState(
            NO_SPONSER, 
            NO_COLLATERAL, 
            NO_NOISE, 
            sample.sampleHeight,
            sample.sampleSize);
        emit InitiateMerkleProof(_msgSender(), root, difficulty, height, reward);
        return true;
    }

    /* The punishment algorthim does not search for the closest blockhash
    * that is stored in the contract, as a result, it may punish users more
    * severly than they actually need to be punished for. This function
    * allows users to point the punishment algorthim in the right direction
    * before it lays down a heavy hand.
    */
    function salvageNoise(uint32 friend, uint32 newBlock) external returns(bool) {
        uint32 oldBlock = _salvage[friend];
        if(oldBlock == newBlock){return false;}
        if((newBlock-oldBlock)%256!=0) {return false;} 
        if(oldBlock != NO_RESCUE && oldBlock <= newBlock) {return false;}
        if(_noise[newBlock] == EMPTY){
            bytes32 noise = blockhash(newBlock);
            if(noise == EMPTY) {return false;}
            _noise[newBlock] = noise;
        }
        _salvage[friend] = newBlock;
        return true;
    }
    function requireSalvageNoise(uint32 friend, uint32 newBlock) external
    {require(this.salvageNoise(friend, newBlock),"W2HP: Can not salvage noise.");}

    /**
    * Tries to record the blockhashes of blocks start (inclusive) to end (exclusive).
    * Returns the amount of blockhashes sucessfully store to the contract.
    */
    function recordNoise(uint32 start, uint32 end) external returns (uint16) {
        uint16 ret = 0;
        bytes32 randomness;
        for(;start < end; start++) {
            randomness = blockhash(start);
            if(randomness != 0) {
                ret++;
                _noise[start] = randomness;
            }
        }
        return ret;
    }
    function requireRecordNoise(uint32 start, uint32 end) external
    {require(this.recordNoise(start,end) != 0, "W2HP: No noise recordable.");}

    //gets the best subtree height and subtree samples.
    uint64 internal constant MAX_CALLDATA = type(uint64).max;
    function distributeSamples(uint8 height, uint64 count) internal pure returns(MerkleSample memory) {
        uint8 subHeight;
        uint8 samples;
        uint64 minCallData = MAX_CALLDATA;
        for(uint8 h = 16<height?16:height-1; h > 0; h--) {
            uint8 newSamples = getSamples(h, count);
            uint64 newCallData = getCallData(height, h, newSamples);
            if(newCallData<minCallData && newSamples*2<2**(height-h)) {
                subHeight = h;
                samples = newSamples;
            }
        }
        return MerkleSample(subHeight,samples);
    }

    // This function calculates the subtree visits needed to visit n leaves
    function getSamples(uint8 height, uint64 count) internal pure returns(uint8) {
        return uint8((count+2**height-1)/(2**height));
    }
    //This function calculates the call data needed to verify a merkle tree proof
    function getCallData(uint8 height, uint8 subHeight, uint8 samples) internal pure returns(uint64) {
        uint64 branchBytes = (height-subHeight)*32;
        uint64 subTreeBytes = uint64(((2**subHeight)*4)+branchBytes);
        return subTreeBytes * samples;
    }

    event InitiateMerkleProof(address miner, bytes32 root, uint256 difficulty, uint8 height, uint16 indexed reward);
}