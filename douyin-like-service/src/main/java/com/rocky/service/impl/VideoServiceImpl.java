package com.rocky.service.impl;

import com.rocky.utils.BaseInfoProperties;
import com.rocky.bo.VideoBO;
import com.rocky.mapper.VideoMapper;
import com.rocky.pojo.Video;
import com.rocky.service.CommentService;
import com.rocky.service.FavoriteService;
import com.rocky.service.UsersService;
import com.rocky.service.VideoService;
import com.rocky.vo.UsersVO;
import com.rocky.vo.VideoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class VideoServiceImpl extends BaseInfoProperties implements VideoService {
    //todo

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UsersService usersService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private CommentService commentService;


    @Override
    public boolean createVideo(VideoBO videoBO) {
        Video video = new Video();
        video.setVideoStatus(videoBO.getVideoStatus());
        video.setCreatedTime(new Date());
        video.setUpdatedTime(new Date());
        video.setCommentsCount(0L);
        video.setCoverUrl(videoBO.getCoverUrl());
        video.setFavoriteCount(0L);
        video.setTitle(videoBO.getTitle());
        video.setPlayUrl(videoBO.getPlayUrl());
        video.setUid(videoBO.getUid());
        videoMapper.insert(video);
        return true;
    }

    @Override
    public VideoVO getVideoVODetail(long sourceUserId, long vid) {
        Video video = videoMapper.selectByPrimaryKey(vid);
        long videoId = video.getId();
        long targetUserId = video.getUid();
        boolean isFavorite = favoriteService.doesUserLikeVideo(sourceUserId,videoId);
        long  commentCount = commentService.getVideoCommentsCount(videoId);
        long favoriteCount = favoriteService.getVideoBeLikedCount(videoId);
        UsersVO usersVO = usersService.findById(sourceUserId,targetUserId);
        VideoVO videoVO = new VideoVO(video,usersVO,favoriteCount,commentCount,isFavorite);
        return videoVO;
    }

    @Override
    public List<VideoVO> getAllVideoList(long sourceUserId, long targetUserId) {
        Example videoExample= new Example(Video.class);
        Example.Criteria criteria = videoExample.createCriteria();
        criteria.andEqualTo("uid", targetUserId);
        List<Video> videoList = videoMapper.selectByExample(videoExample);
        List<VideoVO> videoVOList = new ArrayList<VideoVO>();
        for(Video video: videoList){
            VideoVO videoVO = getVideoVODetail(sourceUserId,video.getId());
            videoVOList.add(videoVO);
        }
        return videoVOList;
    }

    @Override
    public List<VideoVO> findVideoFeed(long sourceUserId, Date endTime) {

        List<Video> videoList  = videoMapper.selectVideoFeedByTime(endTime);

        List<VideoVO> videoVOList = new ArrayList<VideoVO>();
        for(Video video: videoList){
            VideoVO videoVO = getVideoVODetail(sourceUserId,video.getId());
            videoVOList.add(videoVO);
        }
        return videoVOList;

    }

    @Override
    public Date findDateById(long videoId) {

        Video video = videoMapper.selectByPrimaryKey(videoId);
        return video.getCreatedTime();
    }

    @Override
    public long findUserIdByVideoId(long videoId) {

        Video video = videoMapper.selectByPrimaryKey(videoId);

        return video.getUid();
    }

    @Transactional
    @Override
    public void flushCounts(long videoID, Integer counts) {
        videoMapper.updateFavoriteCounts(videoID,counts);
    }
}
