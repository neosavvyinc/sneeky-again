import com.github.bigtoast.sbtliquibase.LiquibasePlugin

seq(LiquibasePlugin.liquibaseSettings: _*)

liquibaseUsername := "shoutout"

liquibasePassword := "shoutout"

liquibaseDriver   := "com.mysql.jdbc.Driver"

liquibaseUrl      := "jdbc:mysql://localhost:3306/shoutout"
