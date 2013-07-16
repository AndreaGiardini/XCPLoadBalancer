CREATE DATABASE Xen;
USE Xen;

CREATE TABLE Host (
    UUID CHAR(36) PRIMARY KEY, --Host UUID
	MemoryTotalMb INT, --Total Physical Memory (MB)
	MemoryFreeMb INT, --Total Free Memory (MB)
	XapiMemoryUsageMb INT, --Memory allocated by the Xapi daemon (MB)
	XapiFreeMemoryMb INT, --Free Momory for the Xapi daemon (MB)
	PifLoRxKb INT, --Received data on the physic interface lo (Kb/s)
	PifLoTxKb INT, --Transmitted data on the physic interface lo (Kb/s)
	PifEth0RxKb INT, --Received data on the physic interface eth0 (Kb/s)
	PifEth0TxKb INT, --Transmitted data on the physic interface eth0 (Kb/s)
	Dom0LoadAvg DOUBLE, --LoadAvg of Dom0
	CpuAverage DOUBLE, --Cpu Average (%)
	Ip VARCHAR(15), --Host Ip
	CpuNum INT, --Cpus number
	Name VARCHAR(100) --Hostname
);
	   
CREATE TABLE Vm (
	UUID CHAR(36) PRIMARY KEY, --Vm UUID
	MemoryMb INT, --Total Virtual Memory (MB)
	MemoryInternalFreeMb INT, --Free Virtual Memory (MB)
	Vif0TxKb INT, --Transmitted data on the virtual interface (Kb/s)
	Vif0RxKb INT, --Received datas on the virtual interface (Kb/s)
	VcpuAverage DOUBLE, --VCpu Average (%)
	VcpuNum INT, --VCpus number
	PowerState VARCHAR(20), --Vm's Power State
	IsControlDomain BOOLEAN, --Check if it's a control domain
	Name VARCHAR(100) --Hostname
);

CREATE TABLE Map (
	HostUuid CHAR(36), --Host UUID
	VmUuid CHAR(36), --Vm UUID
	primary key(`HostUuid`, `VmUuid` ),
	foreign key(`HostUuid`) references Host(`UUID`),
	foreign key(`VmUuid`) references Vm(`UUID`)
);

CREATE TABLE VmRule (
	Subject VARCHAR(100) NOT NULL,
	Threshold DOUBLE NOT NULL
);

CREATE TABLE HostRule (
	Subject VARCHAR(100) NOT NULL,
	Threshold DOUBLE NOT NULL
);

GRANT ALL ON Xen.* TO foo@'%' IDENTIFIED BY 'bar';
