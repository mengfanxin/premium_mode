package com.company;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.lang.*;


/**
 * Created by Administrator on 2017/4/5 0005.
 * 保额公式计算未完成，暂时先不用
 */
public class Calculate {

    public static void main(String[] args) throws SQLException {
        Integer planID = 10022;
        List list = create_tmptable(planID);
        System.out.println(list);
        System.out.println(list.size());



        //遍历临时表
        Iterator rs = list.iterator();
        while(rs.hasNext()){
            Map tmp_map = (Map)rs.next();
//            System.out.println(tmp_map.get("premRateID").toString());
            System.out.println(tmp_map);
            //提取临时表的数据
            Integer premRateID = Integer.valueOf((String) tmp_map.get("premRateID"));
            Integer 保障期限 = Integer.parseInt((String) tmp_map.get("BaoZhangQiXian"));
            Integer age = Integer.parseInt((String) tmp_map.get("age"));
            Integer 缴费年期 = Integer.parseInt((String) tmp_map.get("JiaoFeiNianQi"));
            String sex = (String) tmp_map.get("sex");
//            String sex1 = (String) tmp_map.get("sex1");
//            Integer age1 = Integer.parseInt((String) tmp_map.get("age1"));

//            System.out.println("age1="+age.toString());
//            System.out.println("sex1="+sex1);



            //保单年度
            Integer 保单年度 = 0;
            //当年年龄
            Integer cur_age = 0;
            //上一年的保单期末有效率
            double 上一年的保单期末有效率 = 0;
            //上一年的保费期初有效率
            double 上一年的保费期初有效率 = 0;
            //有效责任给付现值乘积
            double 有效责任给付现值乘积 = 0;
            //保费期初有效率
            double 保费期初有效率 = 0;
            //发生率 临时表
            List FaShengLv_list = new ArrayList();




            //【变量定义】下面这几个变量是计算的时候用到的。也就是【联合计算】和【分开计算】共用的
            //有效责任给付现值 double
            double 有效责任给付现值 = 0;


	        /*这几个是和发生率有关的*/

            //本年保单期末有效率的临时和 double
	        double 本年保单期末有效率的临时和 = 0;
            //保费缴纳否 int
            Integer 保费缴纳否 = 0;
            //发生率比例
            Integer 发生率比例 = 0;
            //发生率
            double 发生率 = 0;
            //condition
            String condition = "";


	        /*这几个是算保额相关的*/
	        //geifufangshi 给付方式
            Integer 给付方式 = 0;
            //SA
            double SA = 0;
            //geifubili 给付比例
            double 给付比例 = 0;
            //isAbsAmount 是否绝对保额
            Integer 是否绝对保额 = 0;
            //amount_value 保额值
            double 保额值 = 0;
            //amountFormula 保额公式
            String 保额公式 = "";
            //amount_sqls
            String amount_sqls = "";
            //当前责任_保额
            double 当前责任_保额 = 0;

	        /*其他计算相关*/
            //保单期末有效率
            double 保单期末有效率 = 0;

	        /*给付现值相关*/
	        //当前责任_给付现值
            double 当前责任_给付现值 = 0;
            //sqls_cur_liab
            String sqls_cur_liab = "";
            //期中折现率
            double 期中折现率 = 0;
            //定价利率
            double 定价利率 = 0;

            /*--==【程序计算开始】==--*/

            /* 遍历【责任】，看看有没有关于“死”的字眼，有的话，就把计算方式换成1 （）*/


            //计算方式，如果计算方式等于1，就联合算，如果是0，就分开算。联合算就是带身故和死亡责任的
            Integer 计算方式 = 0;
            //责任数量
            Integer 责任数量 = 0;
            //包含身故责任的set
            Set set_ShenGuLiab = new HashSet();
            set_ShenGuLiab.add(1);
            set_ShenGuLiab.add(2);
            set_ShenGuLiab.add(3);
//            set_ShenGuLiab.add(9999);

            //开始查询数据找出这条planID下的liab
            String SQL_get_liab_by_plan = "select a.liabID,b.categoryID from [inschos_test].[dbo].[insc_plan_liab] a left join [inschos_test].[dbo].[insc_liability] b on b.id= a.liabID where a.planID = "+ planID;
            ResultSet rs_get_liab_by_plan = SqlHelper.executeQuery(SQL_get_liab_by_plan);
            while (rs_get_liab_by_plan.next()){
                责任数量 = 责任数量 + 1;
                //判断categoryID在没在身故组合里，在的话，设置计算方式为1
                Integer categoryID = Integer.parseInt(rs_get_liab_by_plan.getString("categoryID"));
                if(set_ShenGuLiab.contains(categoryID)){
                    计算方式 = 1;
                }
            }

            System.out.println("计算方式:"+计算方式);

            //【联合计算】如果责任大于1，如果里面有关于“死”的，就联合计算,然后没有关于死的，就分开计算
            if(责任数量 >1 && 计算方式 == 1){

                //---------------开始联合计算(这里只计算，这4个参数全有的情况，其它情况先忽略)
                cur_age = age;
                while(保单年度 < 保障期限+1){
                    Map FaShengLv_map = new HashMap();
                    //投保年龄 int
                    FaShengLv_map.put("age",0);
                    //保单期初有效率 double
                    FaShengLv_map.put("保单期初有效率",0);
                    //保单期末有效率 double
                    FaShengLv_map.put("保单期末有效率",0);
                    //期中折现率 double
                    FaShengLv_map.put("期中折现率",0);
                    //保费期初有效率 double
                    FaShengLv_map.put("保费期初有效率",0);
                    //本年保单期末有效率的临时和 double
                    FaShengLv_map.put("本年保单期末有效率的临时和",0);
                    //保费缴纳否 int
                    FaShengLv_map.put("保费缴纳否",0);

                    本年保单期末有效率的临时和 = 0;

                    保单年度 = 保单年度+1;
                    //这里的年度第一年是31了，而不是30
                    cur_age = cur_age + 1;


					/*定义[保费缴纳否] --最后一步计算净保费用的,计算责任保额时也能用到
                            --这个保费缴纳否，能计算出来，=IF(A9<=Pterm,1,0)  也就是你交到第几年*/

                    if(保单年度 <= 缴费年期){
                        保费缴纳否 = 1;
                    }else{
                        保费缴纳否 = 0;
                    }

                    //--先插入一条记录
                    //insert into #发生率 (age,保费缴纳否)values(@cur_age,@保费缴纳否)
                    FaShengLv_map.put("age",age);
                    FaShengLv_map.put("cur_age",cur_age);
                    FaShengLv_map.put("保费缴纳否",保费缴纳否);

                    //【联合计算】：遍历【责任】开始-----------------------------*/ --根据方案来取所属的责任,[方案-责任 关联表]
                    String SQL_get_liab_by_plan_LianHeJiSuan = "select a.liabID,b.categoryID from [inschos_test].[dbo].[insc_plan_liab] a left join [inschos_test].[dbo].[insc_liability] b on b.id= a.liabID where a.planID = "+planID;
                    ResultSet rs1 = SqlHelper.executeQuery(SQL_get_liab_by_plan_LianHeJiSuan);
                    while (rs1.next()){
                        							/*责任运算开始*/
                        /*找出【发生率】和【当前责任_保额】以及相关参数，并存到【发生率临时表】里
                         发生率有3种情况，当前责任保额也有3种情况，但是两个东西的3种情况不一样，需要分开处理
                        */
                        Integer categoryID = Integer.parseInt(rs1.getString("categoryID"));
                        Integer liabID = Integer.parseInt(rs1.getString("liabID"));

                        //【发生率】生命表
                        Set FaShengLv_life = new HashSet();
                        FaShengLv_life.add(1);
                        FaShengLv_life.add(2);
                        if(FaShengLv_life.contains(categoryID)){
                            //--计算【发生率】
                            发生率比例 = 1; //@发生率比例从保险责任里出的,暂时不用，设置为1
                            condition = "1";

                            //--取发生率

                            if(sex.equals("男")){
                                String SQL_fashenglv_life = "select cl1 from [insdex_test].[dbo].[insdex_fashenglv_life] where age = "+cur_age;
                                ResultSet rs_fashenglv_life = SqlHelper.executeQuery(SQL_fashenglv_life);
                                while (rs_fashenglv_life.next()){
                                    String cl1 = rs_fashenglv_life.getString("cl1");
                                    发生率 = 发生率比例 * Double.valueOf(cl1) / 1000000;
                                }

                            }
                            if(sex.equals("女")){
                                String SQL_fashenglv_life = "select cl2 from [insdex_test].[dbo].[insdex_fashenglv_life] where age ="+ cur_age;
                                ResultSet rs_fashenglv_life = SqlHelper.executeQuery(SQL_fashenglv_life);
                                while (rs_fashenglv_life.next()){
                                    String cl1 = rs_fashenglv_life.getString("cl2");
                                    发生率 = 发生率比例 * Double.valueOf(cl1) / 1000000;
                                }
                            }

                        }



                        //【发生率】6种重疾
                        Set FaShengLv_6ill = new HashSet();
                        FaShengLv_6ill.add(1);
                        FaShengLv_6ill.add(2);
                        if(FaShengLv_6ill.contains(categoryID)){
                            //--计算【发生率】
                            发生率比例 = 1; //@发生率比例从保险责任里出的,暂时不用，设置为1
                            condition = "1";
                            //--取发生率
                            if(sex.equals("男")){
                                String SQL_fashenglv_6ill_select = "select ix1 from [insdex_test].[dbo].[insdex_fashenglv_25sort] where age = "+cur_age;
                                ResultSet rs_fashenglv_6ill_select = SqlHelper.executeQuery(SQL_fashenglv_6ill_select);
                                while (rs_fashenglv_6ill_select.next()){
                                    String ix1 = rs_fashenglv_6ill_select.getString("ix1");
                                    发生率 = 发生率比例 * Double.valueOf(ix1) / 1000000;
                                }
                            }
                            if(sex.equals("女")){
                                String SQL_fashenglv_6ill_select = "select ix2 from [insdex_test].[dbo].[insdex_fashenglv_25sort] where age ="+ cur_age;
                                ResultSet rs_fashenglv_6ill_select = SqlHelper.executeQuery(SQL_fashenglv_6ill_select);
                                while (rs_fashenglv_6ill_select.next()){
                                    String ix2 = rs_fashenglv_6ill_select.getString("ix2");
                                    发生率 = 发生率比例 * Double.valueOf(ix2) / 1000000;
                                }
                            }
                        }
                        //【发生率】25种疾病
                        Set FaShengLv_25ill = new HashSet();
                        FaShengLv_25ill.add(1);
                        FaShengLv_25ill.add(2);
                        if(FaShengLv_25ill.contains(categoryID)){
                            //--计算【发生率】
                            发生率比例 = 1; //@发生率比例从保险责任里出的,暂时不用，设置为1
                            condition = "1";
                            //--取发生率
                            if(sex.equals("男")){
                                String SQL_fashenglv_25ill_select = "select ix1 from [insdex_test].[dbo].[insdex_fashenglv_25sort] where age = " + cur_age;
                                ResultSet rs_fashenglv_25ill_select = SqlHelper.executeQuery(SQL_fashenglv_25ill_select);
                                while (rs_fashenglv_25ill_select.next()){
                                    String ix1 = rs_fashenglv_25ill_select.getString("ix1");
                                    发生率 = 发生率比例 * Double.valueOf(ix1) / 1000000;
                                }
                            }
                            if(sex.equals("女")){
                                String SQL_fashenglv_25ill_select = "select ix2 from [insdex_test].[dbo].[insdex_fashenglv_25sort] where age ="+ cur_age;
                                ResultSet rs_fashenglv_25ill_select = SqlHelper.executeQuery(SQL_fashenglv_25ill_select);
                                while (rs_fashenglv_25ill_select.next()){
                                    String ix2 = rs_fashenglv_25ill_select.getString("ix2");
                                    发生率 = 发生率比例 * Double.valueOf(ix2) / 1000000;
                                }
                            }
                        }
                        //【发生率】期满给付
                        Set FaShengLv_qiman = new HashSet();
                        FaShengLv_qiman.add(1);
                        FaShengLv_qiman.add(2);
						/* --【期满给付】发生率-- */  //--取【上一年的保单期末有效率】 --这种情况不参与保单期末有效率的临时和的计算
//                                --这个暂时不用
//                        if(@categoryID = 9999)
//                        begin
//                        if exists(select 保单期末有效率 from #发生率 where age = (@cur_age-1))
//                        select @上一年的保单期末有效率 = 保单期末有效率 from #发生率 where age = (@cur_age-1)
//											else
//                        set @上一年的保单期末有效率 = 1
//                        set @发生率 = @上一年的保单期末有效率
//
//                        --存临时表(先加责任列，再把责任列加上数据)
//                        exec(''alter table #发生率 add 发生率_责任_'' + @liabID + '' nvarchar(300)'')
//                        exec(''update #发生率 set 发生率_责任_'' +@liabID+ '' = ''''+ @发生率 + '''' where age = @cur_age'')
//                        end

                        //【发生率】意外事故
                        Set FaShengLv_YiWai = new HashSet();
                        FaShengLv_YiWai.add(1);
                        FaShengLv_YiWai.add(2);
                        if(FaShengLv_YiWai.contains(categoryID)){
                            //--计算【发生率】
                            发生率比例 = 1; //@发生率比例从保险责任里出的,暂时不用，设置为1
                            condition = "1";
                            //--取发生率
                            String SQL_fashenglv_yiwai_select = "select value from [insdex_test].[dbo].[insdex_yiwai] where name='全残'";
                            ResultSet rs_fashenglv_yiwai_select = SqlHelper.executeQuery(SQL_fashenglv_yiwai_select);
                            while (rs_fashenglv_yiwai_select.next()){
                                String value = rs_fashenglv_yiwai_select.getString("value");
                                发生率 = 发生率比例 * Double.valueOf(value) / 10000;
                            }
                        }

                        //--存临时表(先加责任列，再把责任列加上数据)
                        try{
                            FaShengLv_map.put("发生率_责任_"+liabID,发生率);
                            FaShengLv_map.put("condition",condition);
                        }catch (Exception e){
                            System.out.println("添加和更新字段出错,有可能是liab不全");
                        }

                        本年保单期末有效率的临时和 = 发生率 + 本年保单期末有效率的临时和;


                        //【联合计算】：==计算【当前责任_保额】==
                        给付方式 = 1;
                        给付比例 = 0.2;
                        SA = 0;
                        //--计算【@SA】(也就是excel里的保额参数)
                        //--下面是从【保额表】里的公式里计算的
                        //--这里从保额公式里取
                        String SQL_amount_select = "select isAbsAmount,value,amountFormula from [inschos_test].[dbo].[insc_amount] where planID = "+planID+" and liabID = "+liabID;
                        ResultSet rs_amount_select = SqlHelper.executeQuery(SQL_amount_select);
                        while (rs_amount_select.next()){
                            是否绝对保额 = Integer.valueOf(rs_amount_select.getString("isAbsAmount"));
                            保额公式 = rs_amount_select.getString("amountFormula");
                            保额值 = Double.valueOf(rs_amount_select.getString("value"));
                        }
                        //如果是绝对保额(这里没完成)
                        if(是否绝对保额==0){
                            //--如果不是绝对保额，则需要通过解析“--”来计算
                            String[] amountFormula_arr = 保额公式.split("--");
                            //=======================================这里计算未完成===========================================================
//                            declare @tmp1 nvarchar(100)
//                            declare @tmp2 nvarchar(200)
//                                    --@tmp1是表名
//                            select @tmp1 = LEFT(@amountFormula,charindex(''--'',@amountFormula,1)-1)
//                            --@tmp2是参数名
//                                    select @tmp2 = SUBSTRING(@amountFormula,charindex(''--'',@amountFormula,1)+2,len(@amountFormula))
//                            --这里有问题，如果从保额表里取，where 可以带上planID和liabID，但是如果参数是从premium_rate里取的话，没有liabID。
//                            --还有问题就是，取出来的值是不是EXCEL里的SA(保额)
//
//                            set @amount_sqls = ''select @sa = ''+@tmp2+ '' from [inschos_test].[dbo].''+@tmp1 + '' where planID = '' +@planID
//                            exec sp_executesql @amount_sqls,N''@sa float output'',@SA output
                        }
                        if(是否绝对保额==1){
                            SA = 保额值;
                        }
///* 这段暂时不用


//											/*定义【保险责任倍数】*/   --就是责任表里的[给付比例]除以100
//                        DECLARE @保险责任倍数 float
//                        SET @保险责任倍数 = @geifubili
//
//
//											/*定义【给付方式】*/
//                                DECLARE @给付方式 int
//                        SET @给付方式 = @geifufangshi
//
//
//
//											/*定义【累计保费缴纳期数】*/   --此值等于    =   上一年的累计保费缴纳期数+这一年的保费缴纳否
//                        DECLARE @累计保费缴纳期数 int
//                        DECLARE @上一年的累计保费缴纳期数 int
//                        if(@上一年的累计保费缴纳期数 is null)
//                        set @上一年的累计保费缴纳期数 = 0
//
//                        SET @累计保费缴纳期数 = @上一年的累计保费缴纳期数 + @保费缴纳否
//                        --上一年的累计保费缴纳期数，赋值给，准备给一下次循环用
//                        SET @上一年的累计保费缴纳期数 = @累计保费缴纳期数
//
//
//
//
//											/*【现金价值_年末】*/ --应该去现金价值表里找，这里先用1000代替吧!
//                                DECLARE @现金价值_年末 int
//                        SET @现金价值_年末 = 1000
//                                */

                        当前责任_保额 = SA;
                        //  /*【责任_保额】入库*/ --判断列名是否存在，不存在则ALTER TABLE加入列
                        try{
                            FaShengLv_map.put("保额_责任_"+liabID,当前责任_保额);
                        }catch (Exception e){
                            System.out.println("局部不错，但是正常，不影响使用");
                        }
                        //exec(''update #发生率 set 保额_责任_'' +@liabID+ '' = ''''''+ @当前责任_保额 + '''''' where age = ''+@cur_age)
                        FaShengLv_map.put("保额_责任_"+liabID,当前责任_保额);


                    }
                    //【联合计算】：计算【当前年龄】的【保单期末有效率】和【期中折现率】并存入【临时表】
                    /*当前年龄所有责任遍历完毕后，计算【当前年龄】的【保单期末有效率】和【期中折现率】并存入【临时表】*/
                    //--先计算【保单期末有效率】=IF($A9>Bterm+1,0,AB7*(1-SUMPRODUCT(U8:Z8,U$1:Z$1)))
                    //--declare @保单期末有效率 float


                    Boolean tmp_flag1 = true;
                    Map tmp_map1 = new HashMap();
                    try{
                        //找出上一年的记录
                        Integer tmp_list_size = FaShengLv_list.size();
                        tmp_map1 = (Map) FaShengLv_list.get(tmp_list_size-1);
                        tmp_flag1 = true;
                    }catch (Exception e){
                        上一年的保单期末有效率 = 1;
                        tmp_flag1 = false;
                    }
                    if(tmp_flag1) {
                        //判断这一年是否超出保单所保年限
                        if (保单年度 > 保障期限 + 1) {
                            保单期末有效率 = 0;
                        } else {
                            //计算上一年的保单期末有效率
                            上一年的保单期末有效率 = 0;

                            //取上年的保单期末有效率。如果没取到就设置为1
                            上一年的保单期末有效率 = Double.valueOf((Double) tmp_map1.get("保单期末有效率"));

                            //保单期末有效率
                            保单期末有效率 = 上一年的保单期末有效率 * (1 - 本年保单期末有效率的临时和);
                        }
                    }


                    //--如果组合里有其它变量，则开始计算
                    //--计算【期中折现率】=(1+pricing_int)^(-A8+0.5)
                    //sqlserv 中power函数是乘方
                    //set @期中折现率 = power((1 + cast(@定价利率 as float)/100), ( -@保单年度 + 0.5))
                    定价利率 = 3.5/100;
                    定价利率 = 3.5;
                    期中折现率 = Math.pow((1 + 定价利率 / 100) , (-保单年度 + 0.5));

                    if(tmp_flag1){
                        if(保单年度 > 保障期限 + 1){
                            保费期初有效率 = 1;
                        }else{
                            //select @保费期初有效率 = (保费期初有效率 * (1 - 本年保单期末有效率的临时和)) from #发生率 where age = (@cur_age-1)
                            本年保单期末有效率的临时和 = Double.valueOf((Double) tmp_map1.get("本年保单期末有效率的临时和"));
                            保费期初有效率 = Double.valueOf((Double) tmp_map1.get("保费期初有效率"));
                            保费期初有效率 = (保费期初有效率 * (1 - 本年保单期末有效率的临时和));
                        }
                    }else{
                        保费期初有效率 = 1;
                    }
                    //--把参数存入【发生率临时表里】
                    //update #发生率 set  本年保单期末有效率的临时和 = @本年保单期末有效率的临时和  where age = @cur_age
                    //update #发生率 set  保费期初有效率 = @保费期初有效率  where age = @cur_age
                    //update #发生率 set  保单期初有效率 = @上一年的保单期末有效率  where age = @cur_age
                    //update #发生率 set  保单期末有效率 = @保单期末有效率  where age = @cur_age
                    //update #发生率 set  期中折现率 = @期中折现率  where age = @cur_age
                    FaShengLv_map.put("本年保单期末有效率的临时和", 本年保单期末有效率的临时和);
                    FaShengLv_map.put("保费期初有效率", 保费期初有效率);
                    FaShengLv_map.put("保单期初有效率", 上一年的保单期末有效率);
                    FaShengLv_map.put("保单期末有效率", 保单期末有效率);
                    FaShengLv_map.put("期中折现率", 期中折现率);


                    //这条应该最后加上
                    FaShengLv_list.add(FaShengLv_map);

                //遍历年龄结束
                }

                //【联合计算】：计算【给付现值】
                //遍历责任，从【发生率临时表】里取数据，并计算各个责任的【给付现值】， 以及【有效责任给付现值】
                /* 遍历【责任】开始，计算【给付现值】 */
                有效责任给付现值 = 0;
                String SQL_geifuxianzhi_liab_select="select a.liabID,b.categoryID from [inschos_test].[dbo].[insc_plan_liab] a left join [inschos_test].[dbo].[insc_liability] b on b.id= a.liabID where a.planID = "+planID;
                ResultSet rs_geifuxianzhi_liab_select = SqlHelper.executeQuery(SQL_geifuxianzhi_liab_select);
                while (rs_geifuxianzhi_liab_select.next()){
                    Integer liabID_geifuxianzhi = rs_geifuxianzhi_liab_select.getInt("liabID");
                    Integer categoryID_geifuxianzhi = rs_geifuxianzhi_liab_select.getInt("categoryID");
                    //--这里又区分3种情况，跟前面发生率的3种情况不一样(一种是两相乘，一种是期满给付的两列相乘，一种是四列相乘)
                    //--第一种情况（【两列相乘】）【责任保额】*【发生率】
                    Set geifuxianzhi_set1 = new HashSet();
                    geifuxianzhi_set1.add(1);
                    geifuxianzhi_set1.add(2);
                    if(geifuxianzhi_set1.contains(categoryID_geifuxianzhi)){
                        当前责任_给付现值 = 0;
                        Iterator lt = FaShengLv_list.iterator();
                        if (lt.hasNext()){
                            Map tmp_map2 = (Map) lt.next();
                            double fashenglv_zeren_liabID = Double.valueOf((Double) tmp_map2.get("发生率_责任_"+liabID_geifuxianzhi));
                            double baoe_zeren_liabID = Double.valueOf((Double) tmp_map2.get("保额_责任_"+liabID_geifuxianzhi));
                            Integer tmp_baofeijiaonafou = Integer.valueOf((Integer) tmp_map2.get("保费缴纳否"));
                            double tmp_sum1 = fashenglv_zeren_liabID * baoe_zeren_liabID * tmp_baofeijiaonafou;
                            当前责任_给付现值 += tmp_sum1;
                        }
                    }
                    //--第二种情况（【期满给付】的【两列相乘】）【责任保额】*【保单期末有效率】
                    Set geifuxianzhi_set2 = new HashSet();
                    geifuxianzhi_set2.add(2);
                    geifuxianzhi_set2.add(3);
                    if(geifuxianzhi_set2.contains(categoryID_geifuxianzhi)){
                        当前责任_给付现值 = 0;
                        Iterator lt = FaShengLv_list.iterator();
                        if (lt.hasNext()){
                            Map tmp_map2 = (Map) lt.next();
                            double baoe_zeren_liabID = Double.valueOf((Double) tmp_map2.get("保额_责任_"+liabID_geifuxianzhi));
                            double tmp_sum1 = 保单期末有效率 * baoe_zeren_liabID;
                            当前责任_给付现值 += tmp_sum1;
                        }
                    }
                    //--第三种情况（【四列相乘】）【责任保额】*【发生率】*【保单期初有效率】*【期中折现率】
                    Set geifuxianzhi_set3 = new HashSet();
                    geifuxianzhi_set3.add(4);
                    geifuxianzhi_set3.add(5);
                    if(geifuxianzhi_set3.contains(categoryID_geifuxianzhi)){
                        当前责任_给付现值 = 0;
                        Iterator lt = FaShengLv_list.iterator();
                        if (lt.hasNext()){
                            Map tmp_map2 = (Map) lt.next();
                            double fashenglv_zeren_liabID = Double.valueOf((Double) tmp_map2.get("发生率_责任_"+liabID_geifuxianzhi));
                            double baoe_zeren_liabID = Double.valueOf((Double) tmp_map2.get("保额_责任_"+liabID_geifuxianzhi));
                            Integer tmp_baofeijiaonafou = Integer.valueOf((Integer) tmp_map2.get("保费缴纳否"));
                            double tmp_baodanqichuyouxiaolv = Double.valueOf((Double) tmp_map2.get("保单期初有效率"));
                            double tmp_sum1 = fashenglv_zeren_liabID * baoe_zeren_liabID * tmp_baofeijiaonafou * tmp_baodanqichuyouxiaolv * 期中折现率;
                            当前责任_给付现值 += tmp_sum1;
                        }
                    }
                    /*计算【有效责任给付现值】*/
                    有效责任给付现值 = 有效责任给付现值 + 当前责任_给付现值;

                }

            //联合计算结束
            //分开计算开始
            }else{
            //-----------------------分开计算开始----------------------------------------------------------------------------------------------------------------
                /* 遍历【责任】开始，计算【给付现值】 */
                有效责任给付现值 = 0;
                String SQL_fenkaijisuan_select = "select a.liabID,b.categoryID from [inschos_test].[dbo].[insc_plan_liab] a left join [inschos_test].[dbo].[insc_liability] b on b.id= a.liabID where a.planID = " + planID;
                ResultSet rs_fenkaijisuan_select = SqlHelper.executeQuery(SQL_fenkaijisuan_select);
                while (rs_fenkaijisuan_select.next()){
                    Integer liabID = rs_fenkaijisuan_select.getInt("liabID");
                    Integer categoryID = rs_fenkaijisuan_select.getInt("categoryID");
                    /*【分开计算】--如果没有年龄范围*/ //(这里先忽略了)
                    /*【分开计算】如果有年龄范围：*/ /*生成年龄范围*/

//                    if(@投保年龄 IS not NULL and @保障期限 is not null)
                    cur_age = age;
                    保单年度= 0;
                    while(保单年度 < 保障期限+1){
                        本年保单期末有效率的临时和 = 0;
                        保单年度 = 保单年度 + 1;
                        //--这里的年度第一年是31了，而不是30
                        cur_age = cur_age + 1;
					    /*定义[保费缴纳否]*/ //--最后一步计算净保费用的,计算责任保额时也能用到
                        //--这个保费缴纳否，能计算出来，=IF(A9<=Pterm,1,0)  也就是你交到第几年
                        if(保单年度 <= 缴费年期){
                            保费缴纳否 = 1;
                        }else{
                            保费缴纳否 = 0;
                        }
                        Map FaShengLv_map = new HashMap();
                        //投保年龄 int
                        FaShengLv_map.put("age",0);
                        //保单期初有效率 double
                        FaShengLv_map.put("保单期初有效率",0);
                        //保单期末有效率 double
                        FaShengLv_map.put("保单期末有效率",0);
                        //期中折现率 double
                        FaShengLv_map.put("期中折现率",0);
                        //保费期初有效率 double
                        FaShengLv_map.put("保费期初有效率",0);
                        //本年保单期末有效率的临时和 double
                        FaShengLv_map.put("本年保单期末有效率的临时和",0);
                        //保费缴纳否 int
                        FaShengLv_map.put("保费缴纳否",0);
                        //--先插入一条记录
                        //insert into #发生率 (age,保费缴纳否)values(@cur_age,@保费缴纳否)
                        FaShengLv_map.put("age",age);
                        FaShengLv_map.put("cur_age",cur_age);
                        FaShengLv_map.put("保费缴纳否",保费缴纳否);

                        //-----【分开计算】计算【发生率】开始--------------------
                        //【发生率】生命表
                        Set FaShengLv_life = new HashSet();
                        FaShengLv_life.add(1);
                        FaShengLv_life.add(2);

                        if(FaShengLv_life.contains(categoryID)){
                            //--计算【发生率】
                            发生率比例 = 1; //@发生率比例从保险责任里出的,暂时不用，设置为1
                            condition = "1";

                            //--取发生率

                            if(sex.equals("男")){
                                String SQL_fashenglv_life = "select cl1 from [insdex_test].[dbo].[insdex_fashenglv_life] where age = "+cur_age;
                                ResultSet rs_fashenglv_life = SqlHelper.executeQuery(SQL_fashenglv_life);
                                while (rs_fashenglv_life.next()){
                                    String cl1 = rs_fashenglv_life.getString("cl1");
                                    发生率 = 发生率比例 * Double.valueOf(cl1) / 1000000;
                                }

                            }
                            if(sex.equals("女")){
                                String SQL_fashenglv_life = "select cl2 from [insdex_test].[dbo].[insdex_fashenglv_life] where age ="+ cur_age;
                                ResultSet rs_fashenglv_life = SqlHelper.executeQuery(SQL_fashenglv_life);
                                while (rs_fashenglv_life.next()){
                                    String cl1 = rs_fashenglv_life.getString("cl2");
                                    发生率 = 发生率比例 * Double.valueOf(cl1) / 1000000;
                                }
                            }

                        }
//                        System.out.println("分类ID "+categoryID);

                        //【发生率】6种重疾
                        Set FaShengLv_6ill = new HashSet();
                        FaShengLv_6ill.add(1);
                        FaShengLv_6ill.add(2);
                        FaShengLv_6ill.add(9999);
                        if(FaShengLv_6ill.contains(categoryID)){
                            //--计算【发生率】
                            发生率比例 = 1; //@发生率比例从保险责任里出的,暂时不用，设置为1
                            condition = "1";
                            //--取发生率
                            if(sex.equals("男")){
                                String SQL_fashenglv_6ill_select = "select ix1 from [insdex_test].[dbo].[insdex_fashenglv_25sort] where age = "+cur_age;
                                ResultSet rs_fashenglv_6ill_select = SqlHelper.executeQuery(SQL_fashenglv_6ill_select);
                                while (rs_fashenglv_6ill_select.next()){
                                    String ix1 = rs_fashenglv_6ill_select.getString("ix1");
                                    发生率 = 发生率比例 * Double.valueOf(ix1) / 10000000;
                                }
                            }
                            if(sex.equals("女")){
                                String SQL_fashenglv_6ill_select = "select ix2 from [insdex_test].[dbo].[insdex_fashenglv_25sort] where age ="+ cur_age;
                                ResultSet rs_fashenglv_6ill_select = SqlHelper.executeQuery(SQL_fashenglv_6ill_select);
                                while (rs_fashenglv_6ill_select.next()){
                                    String ix2 = rs_fashenglv_6ill_select.getString("ix2");
                                    发生率 = 发生率比例 * Double.valueOf(ix2) / 10000000;
                                }
                            }
                        }
                        //【发生率】25种疾病
                        Set FaShengLv_25ill = new HashSet();
                        FaShengLv_25ill.add(1);
                        FaShengLv_25ill.add(2);
                        if(FaShengLv_25ill.contains(categoryID)){
                            //--计算【发生率】
                            发生率比例 = 1; //@发生率比例从保险责任里出的,暂时不用，设置为1
                            condition = "1";
                            //--取发生率
                            if(sex.equals("男")){
                                String SQL_fashenglv_25ill_select = "select ix1 from [insdex_test].[dbo].[insdex_fashenglv_25sort] where age = " + cur_age;
                                ResultSet rs_fashenglv_25ill_select = SqlHelper.executeQuery(SQL_fashenglv_25ill_select);
                                while (rs_fashenglv_25ill_select.next()){
                                    String ix1 = rs_fashenglv_25ill_select.getString("ix1");
                                    发生率 = 发生率比例 * Double.valueOf(ix1) / 10000000;
                                }
                            }
                            if(sex.equals("女")){
                                String SQL_fashenglv_25ill_select = "select ix2 from [insdex_test].[dbo].[insdex_fashenglv_25sort] where age ="+ cur_age;
                                ResultSet rs_fashenglv_25ill_select = SqlHelper.executeQuery(SQL_fashenglv_25ill_select);
                                while (rs_fashenglv_25ill_select.next()){
                                    String ix2 = rs_fashenglv_25ill_select.getString("ix2");
                                    发生率 = 发生率比例 * Double.valueOf(ix2) / 10000000;
                                }
                            }

                        }


                        //【发生率】期满给付
                        Set FaShengLv_qiman = new HashSet();
                        FaShengLv_qiman.add(1);
                        FaShengLv_qiman.add(2);
						/* --【期满给付】发生率-- */  //--取【上一年的保单期末有效率】 --这种情况不参与保单期末有效率的临时和的计算
//                                --这个暂时不用
//                        if(@categoryID = 9999)
//                        begin
//                        if exists(select 保单期末有效率 from #发生率 where age = (@cur_age-1))
//                        select @上一年的保单期末有效率 = 保单期末有效率 from #发生率 where age = (@cur_age-1)
//											else
//                        set @上一年的保单期末有效率 = 1
//                        set @发生率 = @上一年的保单期末有效率
//
//                        --存临时表(先加责任列，再把责任列加上数据)
//                        exec(''alter table #发生率 add 发生率_责任_'' + @liabID + '' nvarchar(300)'')
//                        exec(''update #发生率 set 发生率_责任_'' +@liabID+ '' = ''''+ @发生率 + '''' where age = @cur_age'')
//                        end

                        //【发生率】意外事故
                        Set FaShengLv_YiWai = new HashSet();
                        FaShengLv_YiWai.add(1);
                        FaShengLv_YiWai.add(2);
                        if(FaShengLv_YiWai.contains(categoryID)){
                            //--计算【发生率】
                            发生率比例 = 1; //@发生率比例从保险责任里出的,暂时不用，设置为1
                            condition = "1";
                            //--取发生率
                            String SQL_fashenglv_yiwai_select = "select value from [insdex_test].[dbo].[insdex_yiwai] where name='全残'";
                            ResultSet rs_fashenglv_yiwai_select = SqlHelper.executeQuery(SQL_fashenglv_yiwai_select);
                            while (rs_fashenglv_yiwai_select.next()){
                                String value = rs_fashenglv_yiwai_select.getString("value");
                                发生率 = 发生率比例 * Double.valueOf(value) / 10000;
                            }
                        }

                        System.out.println("分开计算_发生率："+发生率);


                        //--存临时表(先加责任列，再把责任列加上数据)
                        try{
                            FaShengLv_map.put("发生率_责任_"+liabID,发生率);
                            FaShengLv_map.put("condition",condition);
                        }catch (Exception e){
                            System.out.println("添加和更新字段出错,有可能是liab不全");
                        }

                        本年保单期末有效率的临时和 = 发生率 + 本年保单期末有效率的临时和;
                        System.out.println("本年保单期末有效率的临时和"+本年保单期末有效率的临时和);


                        //【分开计算】计算【发生率】结束
                        //【分开计算】计算【当前责任_保额】开始
                        给付方式 = 1;
                        给付比例 = 0.2;
                        SA = 0;
                        //--计算【@SA】(也就是excel里的保额参数)
                        //--下面是从【保额表】里的公式里计算的

                        //--这里从保额公式里取
                        String SQL_amount_select = "select isAbsAmount,value,amountFormula from [inschos_test].[dbo].[insc_amount] where planID = "+planID+" and liabID = "+liabID;
                        ResultSet rs_amount_select = SqlHelper.executeQuery(SQL_amount_select);
                        while (rs_amount_select.next()){
                            是否绝对保额 = Integer.valueOf(rs_amount_select.getString("isAbsAmount"));
                            保额公式 = rs_amount_select.getString("amountFormula");
                            保额值 = Double.valueOf(rs_amount_select.getString("value"));
                        }
                        if(是否绝对保额 == 0){
                            //--如果不是绝对保额，需要解析保额公式，通过保额公式进行计算，这里先忽略了;
                            保额公式 = "";
                        }
                        if(是否绝对保额 == 1){
                            SA = 保额值;
                        }
                        当前责任_保额 = SA;
                        //  /*【责任_保额】入库*/ --判断列名是否存在，不存在则ALTER TABLE加入列
                        try{
                            FaShengLv_map.put("保额_责任_"+liabID,当前责任_保额);
                        }catch (Exception e){
                            System.out.println("局部出错，但是正常，不影响使用");
                        }

                        //exec(''update #发生率 set 保额_责任_'' +@liabID+ '' = ''''''+ @当前责任_保额 + '''''' where age = ''+@cur_age)
                        FaShengLv_map.put("保额_责任_"+liabID,当前责任_保额);
                        System.out.println("当前责任_保额 "+当前责任_保额);
                        //【分开计算】计算【当前责任_保额】结束





                        //【分开计算】计算【当前年龄】的【保单期末有效率】和【期中折现率】并存入【临时表】
                        //--先计算【保单期末有效率】=IF($A9>Bterm+1,0,AB7*(1-SUMPRODUCT(U8:Z8,U$1:Z$1)))
                        Boolean tmp_flag1 = true;
                        Map tmp_map1 = new HashMap();
                        try{
                            //找出上一年的记录
                            Integer tmp_list_size = FaShengLv_list.size();
                            tmp_map1 = (Map) FaShengLv_list.get(tmp_list_size-1);
                            tmp_flag1 = true;
                        }catch (Exception e){
                            上一年的保单期末有效率 = 1;
                            tmp_flag1 = false;
                        }
                        if(tmp_flag1) {
                            //判断这一年是否超出保单所保年限
                            if (保单年度 > 保障期限 + 1) {
                                保单期末有效率 = 0;
                            } else {
                                //计算上一年的保单期末有效率
                                上一年的保单期末有效率 = 0;

                                //取上年的保单期末有效率。如果没取到就设置为1
                                上一年的保单期末有效率 = Double.valueOf((Double) tmp_map1.get("保单期末有效率"));

                                //保单期末有效率
                                保单期末有效率 = 上一年的保单期末有效率 * (1 - 本年保单期末有效率的临时和);
                            }
                        }
                        //--如果组合里有其它变量，则开始计算
                        //--计算【期中折现率】=(1+pricing_int)^(-A8+0.5)
                        //sqlserv 中power函数是乘方
                        //set @期中折现率 = power((1 + cast(@定价利率 as float)/100), ( -@保单年度 + 0.5))
                        定价利率 = 3.5/100;
                        定价利率 = 3.5;
                        期中折现率 = Math.pow((1 + 定价利率 / 100) , (-保单年度 + 0.5));


                        if(tmp_flag1){
                            if(保单年度 > 保障期限 + 1){
                                保费期初有效率 = 1;
                            }else{
                                //select @保费期初有效率 = (保费期初有效率 * (1 - 本年保单期末有效率的临时和)) from #发生率 where age = (@cur_age-1)
                                本年保单期末有效率的临时和 = Double.valueOf((Double) tmp_map1.get("本年保单期末有效率的临时和"));
                                保费期初有效率 = Double.valueOf((Double) tmp_map1.get("保费期初有效率"));
                                保费期初有效率 = (保费期初有效率 * (1 - 本年保单期末有效率的临时和));
                            }
                        }else{
                            保费期初有效率 = 1;
                        }
                        //--把参数存入【发生率临时表里】
                        //update #发生率 set  本年保单期末有效率的临时和 = @本年保单期末有效率的临时和  where age = @cur_age
                        //update #发生率 set  保费期初有效率 = @保费期初有效率  where age = @cur_age
                        //update #发生率 set  保单期初有效率 = @上一年的保单期末有效率  where age = @cur_age
                        //update #发生率 set  保单期末有效率 = @保单期末有效率  where age = @cur_age
                        //update #发生率 set  期中折现率 = @期中折现率  where age = @cur_age
                        FaShengLv_map.put("本年保单期末有效率的临时和", 本年保单期末有效率的临时和);
                        FaShengLv_map.put("保费期初有效率", 保费期初有效率);
                        FaShengLv_map.put("保单期初有效率", 上一年的保单期末有效率);
                        FaShengLv_map.put("保单期末有效率", 保单期末有效率);
                        FaShengLv_map.put("期中折现率", 期中折现率);


                        //这条应该最后加上
                        FaShengLv_list.add(FaShengLv_map);
                        System.out.println("asdas:::"+FaShengLv_map);

                    //遍历年龄结束
                    }


                    System.out.println("发生率表："+FaShengLv_list);


                    //【分开计算】：计算【当前责任_给付现值】和【有效责任给付现值】
                    //--这里又区分3种情况，跟前面发生率的3种情况不一样(一种是两相乘，一种是期满给付的两列相乘，一种是四列相乘)
                    //--第一种情况（【两列相乘】）【责任保额】*【发生率】
                    Set geifuxianzhi_fenkai_set1 = new HashSet();
                    geifuxianzhi_fenkai_set1.add(1);
                    geifuxianzhi_fenkai_set1.add(2);
                    if(geifuxianzhi_fenkai_set1.contains(categoryID)){
                        当前责任_给付现值 = 0;
                        Iterator lt = FaShengLv_list.iterator();
                        if (lt.hasNext()){
                            Map tmp_map2 = (Map) lt.next();
                            double fashenglv_zeren_liabID = Double.valueOf((Double) tmp_map2.get("发生率_责任_"+liabID));
                            double baoe_zeren_liabID = Double.valueOf((Double) tmp_map2.get("保额_责任_"+liabID));
                            Integer tmp_baofeijiaonafou = Integer.valueOf((Integer) tmp_map2.get("保费缴纳否"));
                            double tmp_sum1 = fashenglv_zeren_liabID * baoe_zeren_liabID * tmp_baofeijiaonafou;
                            当前责任_给付现值 += tmp_sum1;
                        }
                    }
                    //--第二种情况（【期满给付】的【两列相乘】）【责任保额】*【保单期末有效率】
                    Set geifuxianzhi_fenkai_set2 = new HashSet();
                    geifuxianzhi_fenkai_set2.add(1);
                    geifuxianzhi_fenkai_set2.add(2);
                    if(geifuxianzhi_fenkai_set2.contains(categoryID)){
                        当前责任_给付现值 = 0;
                        Iterator lt = FaShengLv_list.iterator();
                        if (lt.hasNext()){
                            Map tmp_map2 = (Map) lt.next();
                            double baoe_zeren_liabID = Double.valueOf((Double) tmp_map2.get("保额_责任_"+liabID));
                            double tmp_sum1 = 保单期末有效率 * baoe_zeren_liabID;
                            当前责任_给付现值 += tmp_sum1;
                        }
                    }
                    //--第三种情况（【四列相乘】）【责任保额】*【发生率】*【保单期初有效率】*【期中折现率】
                    Set geifuxianzhi_fenkai_set3 = new HashSet();
                    geifuxianzhi_fenkai_set3.add(1);
                    geifuxianzhi_fenkai_set3.add(2);
                    geifuxianzhi_fenkai_set3.add(9999);
                    if(geifuxianzhi_fenkai_set3.contains(categoryID)){
                        System.out.println("liabID   "+ liabID);

                        当前责任_给付现值 = 0;
                        Iterator lt = FaShengLv_list.iterator();
                        if (lt.hasNext()){
                            Map tmp_map2 = (Map) lt.next();
                            System.out.println("此处的map为："+FaShengLv_list);
                            double fashenglv_zeren_liabID = Double.valueOf((Double) tmp_map2.get("发生率_责任_"+liabID));
                            double baoe_zeren_liabID = Double.valueOf((Double) tmp_map2.get("保额_责任_"+liabID));
                            Integer tmp_baofeijiaonafou = Integer.valueOf((Integer) tmp_map2.get("保费缴纳否"));
                            double tmp_baodanqichuyouxiaolv = Double.valueOf((Double) tmp_map2.get("保单期初有效率"));
                            double tmp_sum1 = fashenglv_zeren_liabID * baoe_zeren_liabID * tmp_baofeijiaonafou * tmp_baodanqichuyouxiaolv * 期中折现率;
                            当前责任_给付现值 += tmp_sum1;
                        }
                    }
                    /*计算【有效责任给付现值】*/
                    有效责任给付现值 = 有效责任给付现值 + 当前责任_给付现值;

                    System.out.println("有效责任给付现值 "+有效责任给付现值);

                //遍历责任结束
                }
            //【分开计算】结束
            }
            double result = 0;
            double tmp_sum1 = 0;
            // select @Result = @有效责任给付现值/(sum(保费期初有效率 * 保费缴纳否)) from #发生率
            Iterator lt1 = FaShengLv_list.iterator();

            while(lt1.hasNext()){
                Map tmp_map3 = (Map) lt1.next();
                double tmp_baofeiqichuyouxiaolv1 = (double) tmp_map3.get("保费期初有效率");
                Integer tmp_baofeijiaonafou1 = (Integer) tmp_map3.get("保费缴纳否");
                double tmp_sum0 = tmp_baofeiqichuyouxiaolv1 * tmp_baofeijiaonafou1;
                tmp_sum1 += tmp_sum0;
            }

            result = tmp_sum1;
            //最后将结果入库
            String SQL_ruku = "insert into [insdex_test].[dbo].[insdex_result]  (result,premRateID) values ('"+result+"', '"+premRateID+"')";
            SqlHelper.executeUpdate(SQL_ruku);
            System.out.println(result);

        //遍历临时表结束
        }

    }


    //创建临时表，返回的是个list
    private static List create_tmptable(int planID) throws SQLException {
        List list = new ArrayList();
        //取该plan下的所有premRate
        String SQL = "select id from insc_premium_rate where planID = " + planID;
        ResultSet rs = SqlHelper.executeQuery(SQL);
        String SQL1 = "";
        //遍历费率
        while (rs.next())
        {
            Map map = new HashMap();
            //根据paramRateID取得paramRateParam
            String premRateID = rs.getString("id");
            SQL1 = "select insuredParamID,value,categoryID from insc_premium_rate_param where premRateID=" + premRateID;
//            System.out.println(rs.getString("id"));
            //遍历费率参数,组合map加入到list
            ResultSet rs1 = SqlHelper.executeQuery(SQL1);
            String insuredParamID = "";
            String value = "";
            String categoryID = "";
            while (rs1.next()){
                insuredParamID = rs1.getString("insuredParamID");
                value = rs1.getString("value");
                categoryID = rs1.getString("categoryID");
                switch(insuredParamID)
                {
                    //保障期限
                    case "21":
                        map.put("BaoZhangQiXian",value);
                        break;
                    //性别
                    case "25":
                        map.put("sex",value);
                        break;
                    //投保年龄
                    case "39":
                        map.put("age", value);
                        break;
                    //缴费年期
                    case "42":
                        map.put("JiaoFeiNianQi", value);
                        break;
                }
                map.put("categoryID",categoryID);
            }
            map.put("planID",planID);
            map.put("premRateID",premRateID);
            list.add(map);
        }
        return list;
    }

}
