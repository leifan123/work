/**
 * @author YuLong.Dai
 * @time 2018年5月3日 下午4:29:59
 * TODO
 */
package com.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.RedirectRule;
import com.amazonaws.services.s3.model.RoutingRule;
import com.amazonaws.services.s3.model.RoutingRuleCondition;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetBucketAclRequest;
import com.amazonaws.services.s3.model.SetBucketWebsiteConfigurationRequest;
///////// add by Yang Honggang  
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.Permission;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YuLong.Dai
 * @time 2018年5月3日 下午4:29:59
 */
public class CephFSTest {

	public static void main(String[] args) throws IOException {
	System.out.println("----------");
		//对象设置权限
		/*String bucketName     = "bucket-name";
		String keyName        = "object-key";
		String uploadFileName = "file-name";

		AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());

		AccessControlList acl = new AccessControlList();
		acl.grantPermission(new CanonicalGrantee("d25639fbe9c19cd30a4c0f43fbf00e2d3f96400a9aa8dabfbbebe1906Example"), Permission.ReadAcp);
		acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
		acl.grantPermission(new EmailAddressGrantee("user@email.com"), Permission.WriteAcp);

		File file = new File(uploadFileName);
		s3client.putObject(new PutObjectRequest(bucketName, keyName, file).withAccessControlList(acl));*/

//		    String accessKey = "B8140BNEWR769CO6D6S5";  
//		  String secretKey = "GGzUkb7M5OwT6doEelrXcqlSFUuJj01JQkqsD5pg";  
	
		  String accessKey = "MC5Y9H4KT784PDU2C32U";  
		  String secretKey = "whpecw3Bu58sa7W6C6ipWmjjNedV8pQYTKVKsoWE";
//	 	String accessKey = "04XA3JGUB33PC4LTU3XO";  
//	 	String secretKey = "MHnMLmAESkOjAwi5AejskiWOyqf655lTYb8hAVO8";
		// 如果默认的端口不是80（假如是90）
		// String endpoint = "10.41.25.35:90";
//		String endpoint = "bjoss.xrcloud.net";
		String endpoint = "testoss.xrcloud.net";
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setSignerOverride("S3SignerType");
		clientConfig.setProtocol(Protocol.HTTP);
		AmazonS3 conn = new AmazonS3Client(credentials,clientConfig);
		conn.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
		conn.setEndpoint(endpoint);
		
//		String bucketName = "sdf150899743737";
//		String objectName = "一拳超人.jpg";
//		
//		// 缩放
//		String style = "image/resize,m_fixed,w_100,h_100";
//		GetObjectRequest request = new GetObjectRequest(bucketName, objectName);
//		System.err.println(conn.getS3AccountOwner().getDisplayName());
		
//		System.out.println(conn.getBucketAcl("zhousub12152472405711"));
		
//		String name = "leifd153250898029";
//		
//		AccessControlList acl = new AccessControlList();
//		acl.grantPermission(new CanonicalGrantee("1508997437371"), Permission.FullControl);
//		acl.setOwner(conn.getS3AccountOwner());
//		conn.createBucket("qwergfdsfsd");
//		
//		AccessControlList bucketAcl = conn.getBucketAcl(name);
//		String identifier = bucketAcl.getGrantsAsList().get(0).getGrantee().getIdentifier();
//		System.out.println(identifier);
//		System.out.println(conn.getBucketAcl(name));
//		System.out.println(conn.getObjectAcl(name, "ceph.docx"));
		
//		ObjectListing objs = conn.listObjects(name);
		
//		List<S3ObjectSummary> sums = objs.getObjectSummaries();
//		for (S3ObjectSummary s3 : sums) {
//			System.out.println("\t" + s3.getKey());
//			conn.setObjectAcl(name, s3.getKey(), acl);
//		}
//		
//		AccessControlList bucketAcl = conn.getBucketAcl(name);
//		System.out.println(bucketAcl);
//		conn.createBucket(name);
//		conn.putObject(name, "Service.html", new File("E:/Service.html"));
		List<Bucket> buckets = conn.listBuckets();
        for (Bucket bucket : buckets) {
            System.out.println("Bucket: " + bucket.getName() );
//            AccessControlList acl = new AccessControlList();
//    		acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
//    		acl.setOwner(conn.getS3AccountOwner());
    		// 设置Bucket权限
//    		conn.setBucketAcl(bucket.getName(), acl);
		 ObjectListing objs = conn.listObjects(bucket.getName());
			List<S3ObjectSummary> sums = objs.getObjectSummaries();
			for (S3ObjectSummary s3 : sums) {
//				conn.setObjectAcl(bucket.getName(), s3.getKey(), acl);
				System.out.println("\t" + s3.getKey());
			}
        }
//		
//		System.out.println(conn.getBucketAcl(name));
//		
//		BucketWebsiteConfiguration bucketWebsiteConfiguration = conn.getBucketWebsiteConfiguration(name);
//		System.out.println(bucketWebsiteConfiguration.getIndexDocumentSuffix());
//		System.out.println(bucketWebsiteConfiguration.getErrorDocument());
		
//		BucketWebsiteConfiguration config = new BucketWebsiteConfiguration();
//		config.setIndexDocumentSuffix("index.html"); //自定义Index页
//		config.setErrorDocument("error.html");	//自定义错误页
////		config.setRedirectAllRequestsTo(new RedirectRule());
//		
//		List<RoutingRule> routingRules = new ArrayList<RoutingRule>();
//		RoutingRule rr = new RoutingRule();
//		rr.setCondition(new RoutingRuleCondition().withHttpErrorCodeReturnedEquals("400")); //重定向 错误编码
//		RedirectRule reRule = new RedirectRule();
//		reRule.setHttpRedirectCode(""); //地址
//		reRule.setProtocol("http"); //协议
//		rr.setRedirect(reRule);
//		routingRules.add(rr);
//		RoutingRule rr1 = new RoutingRule();
//		rr.setCondition(new RoutingRuleCondition().withHttpErrorCodeReturnedEquals("404")); //重定向 错误编码
//		reRule.setHttpRedirectCode(""); //地址
//		rr.setRedirect(reRule);
//		routingRules.add(rr1);
//		RoutingRule rr2 = new RoutingRule();
//		rr.setCondition(new RoutingRuleCondition().withHttpErrorCodeReturnedEquals("403")); //重定向 错误编码
//		reRule.setHttpRedirectCode(""); //地址
//		rr.setRedirect(reRule);
//		routingRules.add(rr2);
//		
//		config.setRoutingRules(routingRules); //访问重定向配置
//		SetBucketWebsiteConfigurationRequest request = new SetBucketWebsiteConfigurationRequest(name, config);
//		conn.setBucketWebsiteConfiguration(request);  //设置静态网站托管
		
//		conn.deleteObject(name, "test.txt");
//		conn.putObject("qa11z", "apache-maven-3.5.3-bin.zip", new File("E:/apache-maven-3.5.3-bin.zip"));
		
		
       
        
//		AccessControlList acl = new AccessControlList();
//		acl.grantPermission(new CanonicalGrantee("150010111"), Permission.WriteAcp);
//		acl.grantPermission(new CanonicalGrantee("150010209"), Permission.WriteAcp);
//		acl.setOwner(conn.getS3AccountOwner());
//		// 设置Bucket权限
//		conn.setBucketAcl(name, acl);
//		//设置Object权限
//        conn.setObjectAcl(name, "apache-maven-3.5.3-bin.zip", acl);
        
//        AccessControlList acl = new AccessControlList();
//        acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
//		acl.setOwner(conn.getS3AccountOwner());
		// 设置Bucket权限
//		conn.setBucketAcl(name, acl);
		
		
		
		
		
//		ObjectListing objs = conn.listObjects(name);		
//		List<S3ObjectSummary> sums = objs.getObjectSummaries();
//		for (S3ObjectSummary s3 : sums) {
//			System.out.println("\t" + s3.getKey());
//		}
//		
		
//		conn.createBucket("123");
		
//		AccessControlList acl = new AccessControlList();
//		acl.grantPermission(GroupGrantee.AllUsers, Permission.WriteAcp);
//		acl.setOwner(conn.getS3AccountOwner());
//		// 设置Bucket权限
//		conn.setBucketAcl("44232150010207", acl);
		
//		List<Bucket> buckets = conn.listBuckets();
//		for (Bucket b : buckets) {
//			System.out.println("bucketName:" + b.getName());
//			ObjectListing objs = conn.listObjects(b.getName());		
//			List<S3ObjectSummary> sums = objs.getObjectSummaries();
//			for (S3ObjectSummary s3 : sums) {
//				System.out.println("\t" + s3.getKey());
//			}
//			//bucketName = b.getName();
//		}
		
//		conn.createBucket("dls123789");
		
		
//		conn.deleteObject(name, "testacl/3.text");
//		conn.deleteBucket;

		
		
		// create buckets
		
//		conn.getbucket
//		conn.deleteObject(name, "dection/");
//		AccessControlList acl = new AccessControlList();
//		acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
//		acl.revokeAllPermissions(new CanonicalGrantee("leifen"));
//		acl.grantPermission(new CanonicalGrantee("leifen"), Permission.FullControl);;;
//		acl.grantPermission(new CanonicalGrantee("leifen"), Permission.FullControl);;;
//		acl.grantPermission(new CanonicalGrantee("zhouyi"), Permission.FullControl);;;
		
		
//		acl.setOwner(conn.getS3AccountOwner());
		
//		conn.createBucket("zhofasdfad");
//		conn.setBucketPolicy("zhouyibat", name);
//		conn.setBucketAcl(new SetBucketAclRequest("", CannedAccessControlList.Private));
//		conn.setBucketAcl(name, acl);
//		bucket = conn.createBucket(new CreateBucketRequest("fada").withCannedAcl(CannedAccessControlList.Private));
//		String bucketName = bucket.getName();
//		conn.createBucket(name);
//		conn.setBucketAcl("zhouyibat", acl);
//		conn.putObject(new PutObjectRequest(name, "testacl/3.text", new File("C:/Users/yunrui006/Desktop/dbconfig.properties")).withAccessControlList(acl));
		
		// list buckets
		//重复不会报错也不会覆盖
//		String bucketName ="";
		/*AccessControlList accesslist = conn.getBucketAcl(name);
		List<Grant> granteeList = accesslist.getGrantsAsList();
		for(Grant grant : granteeList){
			System.out.println("id:  "+grant.getGrantee().getIdentifier());
		}*/
		

		// put Object
//		String objName = "left2.png";
//		conn.putObject(name, "dection12/","");
//		System.out.println(">>> put object: firstobj");

		// get Object
		//.getObject(bucketName, objName);
		//ObjectMetadata se = conn.putObject(new GetObjectRequest(name, "esef/"),"");
 
		// list objects in the bucket

		

//		S3Object downObjc = conn.getObject("zhouyibat", "st.sql");
//		conn.getObject(new GetObjectRequest("zhouyibat", "st.sql"), new File(":/st.sql"));
//		downObjc.getRedirectLocation()
//		conn.getObje
		System.out.println("all done!");
	}
}