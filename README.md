XCPLoadBalancer
===============

XCPLoadBalancer is a set of tools to provide a way to balance an XCP cluster through a CLI.
It's composed by three different small programs that work as explained in the picture:

![ScreenShot](https://raw.github.com/AndreaGiardini/XCPLoadBalancer/master/Interaction.jpg)

 - XCP_Collect - It gets all the informations about the XCP pool's status throught some
API calls to the master node, it checks those informations and populate a Mysql Database
using some queries. You can see the DB structure inside the SQLQuery.sql file.

 - XCP_Manager - It's a command line tool that helps the pool administrator to look at
the pool's status and helps him to define customs rules for balancing.

 - XCP_Control - Periodically checks that all the rules are correctly applied and, 
if one or more is not respected, migrates the virtual machines to balance the load.
 
Feel free to edit/correct/pull/clone everything... this code needs a lot of testing and improvements!

