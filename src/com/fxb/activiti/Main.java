package com.fxb.activiti;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.repository.*;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

public class Main {

    /**
     * 使用框架提供的自动建表（不提供配置文件）
     */
    @Test
    public void test01(){
        ProcessEngineConfiguration conf=ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
        // 设置数据源信息
        conf.setJdbcDriver("com.mysql.jdbc.Driver");
        conf.setJdbcUrl("jdbc:mysql:///activitidemo");
        conf.setJdbcUsername("root");
        conf.setJdbcPassword("root");

        // 设置自动建表
        conf.setDatabaseSchemaUpdate("true");

        // 创建一个流程引擎对象，在创建流程引擎对象过程中会自动建表
        ProcessEngine processEngine = conf.buildProcessEngine();
    }

    /**
     * 使用框架提供的自动建表（提供配置文件）---可以从框架提供的例子程序中获取
     */
    @Test
    public void test02() {
        // 配置文件名称
        String resource = "activiti-context.xml";
        // 配置id值
        String beanName = "processEngineConfiguration";
        ProcessEngineConfiguration conf = ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource(resource,
                        beanName);
        ProcessEngine processEngine = conf.buildProcessEngine();
    }

    /**
     * 使用框架提供的自动建表（使用配置文件）
     */
    @Test
    public void test03() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
    }

    ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

    /**
     * 部署流程定义 方式一：读取单个的流程定义文件 方式二：读取zip压缩文件
     */
    @Test
    public void test1() {
        DeploymentBuilder deploymentBuilder = processEngine.getRepositoryService().createDeployment();

        // 方式一：读取单个的流程定义文件
        /*deploymentBuilder.addClasspathResource("test1.bpmn");
        deploymentBuilder.addClasspathResource("test1.png");
        Deployment deployment = deploymentBuilder.deploy();*/

        // 方式二：读取zip压缩文件
		ZipInputStream zipInputStream = new ZipInputStream(this.getClass().getClassLoader().getResourceAsStream("process.zip"));
		deploymentBuilder.addZipInputStream(zipInputStream);
		deploymentBuilder.name("请假流程部署");
		Deployment deployment = deploymentBuilder.deploy();
    }

    /**
     * 查询部署列表
     */
    @Test
    public void test2() {
        System.out.println("==============================================查询部署列表==start==");
        // 部署查询对象，查询表act_re_deployment
        DeploymentQuery query = processEngine.getRepositoryService()
                .createDeploymentQuery();
        List<Deployment> list = query.list();
        for (Deployment deployment : list) {
            String id = deployment.getId();
            System.out.println(id);
        }
        System.out.println("==============================================查询部署列表==end==");
    }

    /**
     * 查询流程定义列表
     */
    @Test
    public void test3() {
        System.out.println("==============================================查询流程定义列表==start==");
        ProcessDefinitionQuery query = processEngine.getRepositoryService().createProcessDefinitionQuery();
        List<ProcessDefinition> list = query.list();
        list.forEach(e-> System.out.println(e.getName()+"==="+e.getId()));
        System.out.println("==============================================查询流程定义列表==end==");
    }

    /**
     * 删除部署信息
     */
    @Test
    public void test4() {
        System.out.println("==============================================删除部署信息==start==");
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        deployments.forEach(e->processEngine.getRepositoryService().deleteDeployment(e.getId()));
        System.out.println("==============================================删除部署信息==end==");
    }

    /**
     * 删除流程定义(通过删除部署信息达到删除流程定义的目的)
     */
    @Test
    public void test5() {
        System.out.println("==============================================删除流程定义==start==");
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        deployments.forEach(e->processEngine.getRepositoryService().deleteDeployment(e.getId(),true));
        System.out.println("==============================================删除流程定义==end==");
    }

    /**
     * 查询一次部署对应的流程定义文件名称和对应的输入流（bpmn png）
     *
     * @throws Exception
     */
    @Test
    public void test6() throws Exception {
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        String deployId=deployments.get(0).getId();
        List<String> deploymentResourceNames = processEngine.getRepositoryService().getDeploymentResourceNames(deployId);
        for (String name : deploymentResourceNames) {
            System.out.println(name);
            InputStream inputStream = processEngine.getRepositoryService().getResourceAsStream(deployId, name);
            /*OutputStream outputStream=new FileOutputStream(new File("D:\\"+name));
            int len;
            byte[] by=new byte[1024];
            while ((len=inputStream.read(by))!=-1){
                outputStream.write(by,0,len);
            }*/
            FileUtils.copyInputStreamToFile(inputStream, new File("D:\\"+name));
            //inputStream.close();
            //outputStream.close();
        }
    }

    /**
     * 获得png文件的输入流
     *
     * @throws Exception
     */
    @Test
    public void test7() throws Exception {
        List<ProcessDefinition> list = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        InputStream processDiagram = processEngine.getRepositoryService().getProcessDiagram(list.get(0).getId());
        FileUtils.copyInputStreamToFile(processDiagram,new File("d:\\"+list.get(0).getDiagramResourceName()));
    }

    /**
     * 启动流程实例 方式一：根据流程定义的id启动 方式二：根据流程定义的key启动(自动选择最新版本的流程定义启动流程实例)
     */
    @Test
    public void test8() {
        System.out.println("==============================================启动流程实例==start==");
        List<ProcessDefinition> list = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        //ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceById(list.get(0).getId());

        ProcessInstance processInstance2 = processEngine.getRuntimeService().startProcessInstanceByKey(list.get(0).getKey());
        System.out.println("==============================================启动流程实例==end==");
    }

    /**
     * 查询流程实例列表,查询act_ru_execution表
     */
    @Test
    public void test9(){
        //流程实例查询对象，查询act_ru_execution表
        ProcessInstanceQuery query = processEngine.getRuntimeService().createProcessInstanceQuery();
        query.processDefinitionKey("qjlc");
        query.orderByProcessInstanceId().desc();
        query.listPage(0, 5);
        List<ProcessInstance> list = query.list();
        for (ProcessInstance pi : list) {
            System.out.println(pi.getId() + " " + pi.getActivityId());
        }
    }

    /**
     * 结束流程实例,操作的表act_ru_execution act_ru_task
     */
    @Test
    public void test10(){
        ProcessInstanceQuery query = processEngine.getRuntimeService().createProcessInstanceQuery();
        processEngine.getRuntimeService().deleteProcessInstance(query.list().get(0).getId(), "我愿意");
    }

    /**
     * 查询任务列表
     */
    @Test
    public void test11(){
        //任务查询对象,查询act_ru_task表
        TaskQuery query = processEngine.getTaskService().createTaskQuery();
        String assignee = "张三";
        query.taskAssignee(assignee);
        query.orderByTaskCreateTime().desc();
        List<Task> list = query.list();
        for (Task task : list) {
            System.out.println(task.getId()+"=="+task.getAssignee());
        }
    }

    /**
     * 办理任务
     */
    @Test
    public void test12(){
        TaskQuery query = processEngine.getTaskService().createTaskQuery();
        processEngine.getTaskService().complete(query.list().get(0).getId());
    }

    /**
     * 直接将流程向下执行一步
     */
    @Test
    public void test13(){
        TaskQuery query = processEngine.getTaskService().createTaskQuery();
        processEngine.getRuntimeService().signal(query.list().get(0).getExecutionId());
    }

    /**
     * 查询最新版本的流程定义列表
     */
    @Test
    public void test14(){
        ProcessDefinitionQuery query = processEngine.getRepositoryService().createProcessDefinitionQuery();
        query.orderByProcessDefinitionVersion().asc();
        List<ProcessDefinition> list = query.list();
        Map<String, ProcessDefinition> map = new HashMap<String, ProcessDefinition>();
        for (ProcessDefinition pd : list) {
            map.put(pd.getKey(), pd);
        }
        ArrayList<ProcessDefinition> lastList = new ArrayList<>(map.values());
        for (ProcessDefinition processDefinition : lastList) {
            System.out.println(processDefinition.getName() + "  "+ processDefinition.getVersion() );
        }
    }

}
