package com.rocky.service.impl;

import com.rocky.bo.MessageBO;
import com.rocky.pojo.Users;
import com.rocky.result.MessageEnum;
import com.rocky.result.ResponseStatusEnum;
import com.rocky.utils.BaseInfoProperties;
import com.rocky.mapper.FollowMapper;
import com.rocky.pojo.Follow;
import com.rocky.service.FollowService;
import com.rocky.service.UsersService;
import com.rocky.utils.JsonUtils;
import com.rocky.utils.RabbitMQConfig;
import com.rocky.vo.FriendUserVO;
import com.rocky.result.ResultVO;
import com.rocky.vo.UsersVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
public class FollowServiceImpl extends BaseInfoProperties implements FollowService {
    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UsersService usersService;

    @Autowired
    public RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public ResultVO follow(long fromUID, long toUID) {
        boolean hasBeenFollowed = isFollow(fromUID, toUID);
        //如果已经关注
        if(hasBeenFollowed){
            return ResultVO.ok(ResponseStatusEnum.SUCCESS);
        }
        boolean isFriend = isFollow(toUID,fromUID);
        Follow follow = new Follow();
        follow.setFromId(fromUID);
        follow.setToId(toUID);
        if(isFriend)
            follow.setIsFriend((byte) 1);
        else
            follow.setIsFriend((byte) 0);
        follow.setFollowStatus((byte) 1);
        follow.setCreateTime(new Date());
        int rs = followMapper.insert(follow);
        if(rs>0){
            // 关注人关注数加一
            usersService.updateFollowCount(fromUID, (byte) 1);
            //被关注人粉丝加一
            usersService.updateFollowerCount(toUID, (byte) 1);
        }
        else
            return ResultVO.error(ResponseStatusEnum.FAILED);
        //todo
        // 系统消息：关注博主
        // MQ异步解耦
        //使用官方账户发送谁关注你了
        MessageBO messageBO = new MessageBO(1L,toUID,"有人关注你了！",(byte)0);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MSG,
                "sys.msg." + MessageEnum.FOLLOW_YOU.enValue,
                JsonUtils.objectToJson(messageBO));

        return ResultVO.ok(ResponseStatusEnum.SUCCESS);
    }

    @Override
    public ResultVO unFollow(long fromUID, long toUID) {
        // 判断是否关注
        boolean hasBeenFollowed = isFollow(fromUID, toUID);
        //如果没有关注，则无需操作
        if(!hasBeenFollowed){
            return ResultVO.ok(ResponseStatusEnum.SUCCESS);
        }
        Follow follow = new Follow();
        follow.setFromId(fromUID);
        follow.setToId(toUID);
        int rs = followMapper.delete(follow);
        if(rs>0){
            usersService.updateFollowCount(fromUID, (byte) 2);
            usersService.updateFollowerCount(toUID, (byte) 2);
        }
        else
            return ResultVO.error(ResponseStatusEnum.FAILED);
        return ResultVO.ok(ResponseStatusEnum.SUCCESS);
    }

    @Override
    public ResultVO getFollowList(long uid) {
        List<Long> toIDList = followMapper.selectFollowListByUID(uid);

        List<UsersVO> userList = new ArrayList<UsersVO>();
        for (Long toID : toIDList) {
            userList.add(usersService.findById(uid, toID));
        }

        ResultVO resultVO = new ResultVO();
        resultVO.setStatusCode(0);
        resultVO.setStatusMsg("成功获取关注列表");
        resultVO.setData(userList);
        resultVO.setObjectName("user_list");

        return resultVO;
    }

    @Override
    public ResultVO getFollowerList(long uid) {
        List<Long> fromIDList = followMapper.selectFollowerListByUID(uid);

        List<UsersVO> userList = new ArrayList<UsersVO>();
        for (Long fromID : fromIDList) {
            userList.add(usersService.findById(uid, fromID));
        }

        ResultVO resultVO = new ResultVO();
        resultVO.setStatusCode(0);
        resultVO.setStatusMsg("成功获取粉丝列表");
        resultVO.setData(userList);
        resultVO.setObjectName("user_list");

        return resultVO;
    }

    @Override
    public ResultVO getFriendList(long uid) {
        List<Long> toIDList = followMapper.selectFriendListByUID(uid);

        List<FriendUserVO> userList = new ArrayList<FriendUserVO>();
        for (Long toID : toIDList) {
            UsersVO usersVO =  usersService.findById(uid, toID);
            FriendUserVO friendUserVO = new FriendUserVO(usersVO,"hello",0L);
            userList.add(friendUserVO);
        }

        ResultVO resultVO = new ResultVO();
        resultVO.setStatusCode(0);
        resultVO.setStatusMsg("成功获取朋友列表");
        resultVO.setData(userList);
        resultVO.setObjectName("user_list");

        return resultVO;
    }

    @Override
    public long getFollowCount(long uid) {
        return followMapper.selectFollowCount(uid);
    }

    @Override
    public long getFollowerCount(long uid) {
        return followMapper.selectFollowerCount(uid);
    }

    @Override
    public boolean isFollow(long fromId, long toId) {
        Follow follow = new Follow();
        follow.setFromId(fromId);
        follow.setToId(toId);

        List<Follow> followList = followMapper.select(follow);
        if (followList.size() == 0) {
            return false;
        }

        Follow result = followList.get(0);
        return result.getFollowStatus() != (byte) 0;
    }




}
