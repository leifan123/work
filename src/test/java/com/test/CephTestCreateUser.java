/**
 * @author YuLong.Dai
 * @time 2018年5月10日 上午11:43:51
 * TODO
 */
package com.test;

import java.util.List;
import java.util.Optional;

import javax.mail.Quota;import org.apache.commons.pool2.UsageTracking;
import org.twonote.rgwadmin4j.RgwAdmin;
import org.twonote.rgwadmin4j.RgwAdminBuilder;
import org.twonote.rgwadmin4j.model.S3Credential;
import org.twonote.rgwadmin4j.model.UsageInfo;
import org.twonote.rgwadmin4j.model.User;

/**
 * @author YuLong.Dai
 * @time 2018年5月10日 上午11:43:51
 */
public class CephTestCreateUser {

	public static void main(String[] args) {
		String accessKey = "NUU8WCP8QN2NT7X63EGL";
		String secretKey = "rePdOD49dEtZrTZsRVY6snNvOKaZR3HAwnlRWStm";
//		 String accessKey = "NI7F0LK8YOWUAXLCG3SF";
//		 String secretKey = "UsLwOrBW5lx6sOiltIIbQEoLlZf61tlgTRDgwCrl";
		String adminEndpoint = "http://183.134.65.81:7480/";
		RgwAdmin rgwAdmin = new RgwAdminBuilder().accessKey(accessKey).secretKey(secretKey).endpoint(adminEndpoint)
				.build();
		

		
		//	String userId = "150010111235";

		// rgwAdmin.setUserQuota(userId,
		// rgwAdmin.getBucketQuota(userId).get().getMaxObjects(), 1024);

		// Optional<User> userInfo = rgwAdmin.getUserInfo(userId);
		//
		// Optional<org.twonote.rgwadmin4j.model.Quota> userQuota =
		// rgwAdmin.getUserQuota(userId);
		// System.out.println(userQuota.get().getMaxObjects() + ", " +
		// userQuota.get().getMaxSizeKb());
		//
		// System.out.println(userInfo.get().getUserId());

		// User user = null;
		// // create a user
		//
		
//		rgwAdmin.createUser("152237560823");
		rgwAdmin.removeUser("150899716136");
		
//		Optional<User> userInfo = rgwAdmin.getUserInfo("152237560823");
//		System.out.println(userInfo.get());
		
//		System.out.println(userInfo.get().getUserId());
		
		User user = rgwAdmin.createUser("156498645");
		if (user != null) {
			// // get user S3Credential
			for (S3Credential credential : user.getS3Credentials()) {
				System.out.println("userid: " + credential.getUserId() + ",getAccessKey: " + credential.getAccessKey()
						+ ", getSecretKey: " + credential.getSecretKey());
			}
		}
		// userid: 150010111,getAccessKey: 69ZXSK3RWB8MXQVYW41R, getSecretKey:
		// 9iyfXzCKvjjkVCMqBTlUzXm0C0cGFKdK0S8OU3Ay
		// // set user quota, such as maxObjects and maxSize(KB)
		// rgwAdmin.setUserQuota(userId, 1000, 1024 * 1024 * 5);
		//
		// } else {
		// //userid: 150010209,getAccessKey: JWS7Q36C7MZ0BAAHQU2S, getSecretKey:
		// 635u3QOltdYM3hEiGDDSDcdgIvbia2rg3UQgjUSE
		// System.out.println("create user failed");
		// }

	}
}
