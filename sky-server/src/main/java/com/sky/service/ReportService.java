package com.sky.service;


import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public interface ReportService {

    //统计指定时间区间内的营业额
    TurnoverReportVO getTurnoverStatistics( LocalDate begin, LocalDate end);

    //统计指定时间区间内的用户数量
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);
}
