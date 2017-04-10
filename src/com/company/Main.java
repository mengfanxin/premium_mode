package com.company;
import java.sql.*;

import com.company.SqlHelper;

public class Main {

    public static void main(String[] args) throws SQLException {

        //查询DEMO
        String SQL1 = "SELECT top 100 id,planID FROM insc_amount ";
        ResultSet rs1 = SqlHelper.executeQuery(SQL1);
        while (rs1.next())
        {
            System.out.println(rs1.getString("id") + ", " + rs1.getString("planID"));
        }

        //插入和更新DEMO
        String SQL2 = "update insc_amount set PremRateID = '99' where id = '57750'";
        if(SqlHelper.executeUpdate(SQL2))
        {
            System.out.println("更新成功 ");
        }else
        {
            System.out.println("更新失败 ");
        }








    }
}
