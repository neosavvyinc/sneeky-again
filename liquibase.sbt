import com.github.bigtoast.sbtliquibase.LiquibasePlugin

seq(LiquibasePlugin.liquibaseSettings: _*)

liquibaseUsername := "sneekyv2"

liquibasePassword := "sneekyv2"

liquibaseDriver   := "com.mysql.jdbc.Driver"

liquibaseUrl      := "jdbc:mysql://localhost:3306/sneekyv2"
