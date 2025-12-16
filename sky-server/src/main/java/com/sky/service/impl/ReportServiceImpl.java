package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            //日期计算，计算指定日期后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的营业额数据
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期对应的营业额数据，营业额：状态是已完成（5）的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status",Orders.COMPLETED);
            //select sum(amount) from orders where order_time > ? and order_time < ? and status = 5
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);

        }


        //封装，返回结果
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();

    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end之间的每天对应的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的新增用户数量
        //select count(id) from user where create_time < ? and create_time > ?
        List<Integer> newUserList = new ArrayList<>();
        //存放每天的总用户数量  select count(id) from user where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();

        //遍历每一天进行统计
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap();

            map.put("end",endTime);
            //先只放end，相当于统计totalUserList数量
            Integer totalUserNum = userMapper.countByMap(map);
            //再放begin,查出总用户数量
            map.put("begin",beginTime);
            Integer newUserNum = userMapper.countByMap(map);

            totalUserList.add(totalUserNum);
            newUserList.add(newUserNum);
        }

        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();

        return userReportVO;
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end之间的每天对应的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的订单数量
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天的有效（完成的）订单数量
        List<Integer> validOrderCountList = new ArrayList<>();

        //遍历dateList集合，查询每天的有效订单数和订单总数
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap();

            //先只放起止时间不放状态，相当于查询每天总的订单数量
            map.put("begin",beginTime);
            map.put("end",endTime);
            //查询每天的订单数 select count(id) from orders where order_time < ? and create_time > ?
            Integer orderCount = orderMapper.countByMap(map);


            //再放状态信息，查询每天有效的订单数量
            map.put("status",Orders.COMPLETED);
            //查询每天的有效订单数 select count(id) from orders where order_time < ? and create_time > ? and status = 5
            Integer validOrderCount = orderMapper.countByMap(map);

            //放到List集合当中
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //totalOrderCount,validOrderCount, orderCompletionRate
        //计算时间区间内的订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //计算订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();


        return orderReportVO;
    }

    /**
     * 统计指定时间区间内的销量排行
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);

        //商品名称列表nameList 销量列表numberList
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        //for循环写法
        /*List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();

        for (GoodsSalesDTO goodsSalesDTO : salesTop10) {
            nameList.add(goodsSalesDTO.getName());
            numberList.add(goodsSalesDTO.getNumber());
        }*/

        //Stream流写法
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");


        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库，获取营业数据 ---- 最近三十天的数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2.通过POI将数据写入到excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //获得excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获得第一页内容
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据 -- （1，1）时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            XSSFRow row3 = sheet.getRow(3);
            //（3，2）营业额 turnover
            row3.getCell(2).setCellValue(businessDataVO.getTurnover());

            //(3,4) 订单完成率 orderCompletionRate
            row3.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());

            //(3,6) 新增用户数 newUsers
            row3.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //(4,2) 有效订单数 validOrderCount
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());

            //(4,4) 平均客单价 unitPrice
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);

                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3.通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            in.close();
            out.close();
            excel.close();


        } catch (IOException e) {
            e.printStackTrace();
        }




    }
}
