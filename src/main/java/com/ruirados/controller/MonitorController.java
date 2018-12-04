package com.ruirados.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ruirados.pojo.CustomMonitorIndex;
import com.ruirados.pojo.MonitorL;
import com.ruirados.pojo.OssBucket;
import com.ruirados.pojo.OssFlux;
import com.ruirados.pojo.OssMonitor;
import com.ruirados.pojo.OssStatistics;
import com.ruirados.pojo.OssUserFlux;
import com.ruirados.pojo.OssZone;
import com.ruirados.pojo.PandectCustomMonitorIndex;
import com.ruirados.pojo.PandectbigCustomMonitorIndex;
import com.ruirados.pojo.RspData;
import com.ruirados.pojo.UserCoreAccess;
import com.ruirados.pojo.UserTypeSource;
import com.ruirados.service.CustomMonitorIndexService;
import com.ruirados.service.OssBucketService;
import com.ruirados.service.OssFluxService;
import com.ruirados.service.OssMonitorService;
import com.ruirados.service.OssStatisticsService;
import com.ruirados.service.OssUserFluxService;
import com.ruirados.service.OssZoneService;
import com.ruirados.service.PandectCustomMonitorIndexService;
import com.ruirados.service.PandectbigCustomMonitorIndexService;
import com.ruirados.service.UserCoreAccessService;
import com.ruirados.service.UserTypeSourceService;
import com.ruirados.util.ApiTool;
import com.ruirados.util.Config;
import com.ruirados.util.ExptNum;
import com.ruirados.util.GetResult;
import com.ruirados.util.JSONUtils;
import com.ruirados.util.MappingConfigura;
import com.ruirados.util.MonitorUtil;
import com.ruirados.util.ParamIsNull;

import net.sf.json.JSONObject;

@Controller
@RequestMapping(MappingConfigura.MONITOR)
public class MonitorController {

	@Autowired
	private OssMonitorService ossMonitorService;

	@Autowired
	private OssBucketService ossBucketService;

	@Autowired
	private UserCoreAccessService userCoreAccessService;

	@Autowired
	private CustomMonitorIndexService customMonitorIndexService;

	@Autowired
	private OssStatisticsService ossStatisticsService;
	
	@Autowired
	private PandectbigCustomMonitorIndexService pandectbigCustomMonitorIndexService;
	
	@Autowired
	private PandectCustomMonitorIndexService pandectCustomMonitorIndexService;
	
	@Autowired
	private UserTypeSourceService typeSourceService;
	
	@Autowired
	private OssFluxService fluxService;
	
	@Autowired
	private OssUserFluxService userFluxService;
	
	@Autowired
	private OssZoneService ossZoneService;

	Logger log = Logger.getLogger(getClass());

	/**
	 * GET请求次数(分别为今天、昨天、七天前、30天)
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "getTimesList", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String getTimesList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, String> maps = ApiTool.getBodyString(request);
		String zoneId = maps.get("zoneId");
		String bucketName = maps.get("bucketName");
		String times = maps.get("times");

		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(times)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();

		String today = sdf.format(date);

		Calendar calendar1 = Calendar.getInstance();
		calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 1);
		Date today1 = calendar1.getTime();

		String yesterday = sdf.format(today1);

		List<OssMonitor> select = null;
		List<String> timestamp = null;
		List<Integer> hourstamp = null;

		// GET请求次数
		List<Integer> getTimesList = new LinkedList<Integer>();

		Map<String, Object> map = new HashMap<String, Object>();

		String companyId = ApiTool.getUserMsg(request).getCompanyid();

		List<String> dateList = new LinkedList<String>();

		select = MonitorUtil.getByParam(ossMonitorService, ossBucketService, userCoreAccessService,
				Integer.parseInt(times), today, bucketName, zoneId, yesterday, companyId);

		if (times.equals("1") || times.equals("2")) {
			hourstamp = MonitorUtil.getByHour(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), today, bucketName, zoneId, yesterday, companyId);
			if (hourstamp.size() == 0 || hourstamp == null) {
				for (int i = 0; i < 24; i++) {
					getTimesList.add(0);
				}
				map.put("getTimes", getTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			if (hourstamp.size() != 24) {
				int i = 0, getTimes = 0;
				for (int hour : hourstamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							getTimesList.add(getTimes);
							getTimes = 0;
						}
						if (monitor.getTime() == hour) {
							if (monitor.getType() == 1) {
								getTimes += monitor.getTimes();
							}
						}
						n++;
					}
					i++;
				}
				getTimesList.add(getTimes);

				for (int k = 0; k < 24; k++) {
					int num = 0;
					for (int j : hourstamp) {
						if (k == j) {
							break;
						}
						if (num == hourstamp.size() - 1 && k != j) {
							try {
								getTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								getTimesList.add(0);
							}
						}
						num++;
					}
				}
			} else {
				int i = 0, getTimes = 0;
				for (int hour : hourstamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							getTimesList.add(getTimes);
							getTimes = 0;
						}
						if (monitor.getTime() == hour) {
							if (monitor.getType() == 1) {
								getTimes += monitor.getTimes();
							}
						}
						n++;
					}
					i++;
				}
				getTimesList.add(getTimes);
			}

			map.put("getTimes", getTimesList);

		} else if (times.equals("3")) {

			dateList = MonitorUtil.getDate(Integer.parseInt(times));

			map.put("dateList", dateList);

			timestamp = MonitorUtil.getByCount(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), bucketName, zoneId, companyId);

			if (timestamp.size() == 0 || timestamp == null) {
				for (int i = 0; i < 7; i++) {
					getTimesList.add(0);
				}
				map.put("getTimes", getTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// timestamp.add(timestamp.get(0));
			if (timestamp.size() != 7) {
				int i = 0, getTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							getTimesList.add(getTimes);
							getTimes = 0;
						}
						if (monitor.getDate().equals(str)) {
							if (monitor.getType() == 1) {
								getTimes += monitor.getTimes();
							}
						}
						n++;
					}
					i++;
				}
				getTimesList.add(getTimes);

				int k = 0;
				for (String string : dateList) {
					int num = 0;
					for (String j : timestamp) {
						if (string.equals(j.substring(j.indexOf("-") + 1))) {
							break;
						}
						if (num == timestamp.size() - 1 && !string.equals(j.substring(j.indexOf("-") + 1))) {
							try {
								getTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								getTimesList.add(0);
							}
						}
						num++;
					}
					k++;
				}
			} else {
				int i = 0, getTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							getTimesList.add(getTimes);
							getTimes = 0;
						}
						if (monitor.getDate().equals(str)) {
							if (monitor.getType() == 1) {
								getTimes += monitor.getTimes();
							}
						}
						n++;
					}
					i++;
				}
				getTimesList.add(getTimes);
			}

			map.put("getTimes", getTimesList);
		} else {

			dateList = MonitorUtil.getDate(Integer.parseInt(times));

			map.put("dateList", dateList);

			timestamp = MonitorUtil.getByCount(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), bucketName, zoneId, companyId);

			if (timestamp.size() == 0 || timestamp == null) {
				for (int i = 0; i < 30; i++) {
					getTimesList.add(0);
				}
				map.put("getTimes", getTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// timestamp.add(timestamp.get(0));

			if (timestamp.size() != 30) {
				int getTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							if (monitor.getType() == 1) {
								getTimes += monitor.getTimes();
							}
						}
						if (n == select.size() - 1) {
							getTimesList.add(getTimes);
							getTimes = 0;
						}
						n++;
					}
				}

				int k = 0;
				for (String string : dateList) {
					int num = 0;
					for (String j : timestamp) {
						if (string.equals(j.substring(j.indexOf("-") + 1))) {
							break;
						}
						if (num == timestamp.size() - 1 && !string.equals(j.substring(j.indexOf("-") + 1))) {
							try {
								getTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								getTimesList.add(0);
							}
						}
						num++;
					}
					k++;
				}
			} else {
				int getTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							if (monitor.getType() == 1) {
								getTimes += monitor.getTimes();
							}
						}
						if (n == select.size() - 1) {
							getTimesList.add(getTimes);
							getTimes = 0;
						}
						n++;
					}
				}
			}

			map.put("getTimes", getTimesList);
		}

		rd.setStatus(ExptNum.SUCCESS.getCode() + "");
		rd.setMsg(ExptNum.SUCCESS.getDesc());
		rd.setData(map);
		return JSONUtils.createObjectJson(rd);
	}

	/**
	 * PUT请求次数(分别为今天、昨天、七天前、30天)
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "putTimesList", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String putTimesList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, String> maps = ApiTool.getBodyString(request);
		String zoneId = maps.get("zoneId");
		String bucketName = maps.get("bucketName");
		String times = maps.get("times");

		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(times)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();

		String today = sdf.format(date);

		Calendar calendar1 = Calendar.getInstance();
		calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 1);
		Date today1 = calendar1.getTime();

		String yesterday = sdf.format(today1);

		List<OssMonitor> select = null;
		List<String> timestamp = null;
		List<Integer> hourstamp = null;

		// PUT请求次数
		List<Integer> putTimesList = new LinkedList<Integer>();

		Map<String, Object> map = new HashMap<String, Object>();

		String companyId = ApiTool.getUserMsg(request).getCompanyid();

		List<String> dateList = new LinkedList<String>();

		select = MonitorUtil.getByParam(ossMonitorService, ossBucketService, userCoreAccessService,
				Integer.parseInt(times), today, bucketName, zoneId, yesterday, companyId);

		if (times.equals("1") || times.equals("2")) {
			hourstamp = MonitorUtil.getByHour(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), today, bucketName, zoneId, yesterday, companyId);
			if (hourstamp.size() == 0 || hourstamp == null) {
				for (int i = 0; i < 24; i++) {
					putTimesList.add(0);
				}
				map.put("putTimes", putTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// hourstamp.add(hourstamp.get(0));
			if (hourstamp.size() != 24) {
				int i = 0, putTimes = 0;
				for (int hour : hourstamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							putTimesList.add(putTimes);
							putTimes = 0;
						}
						if (monitor.getTime() == hour) {
							if (monitor.getType() == 2) {
								putTimes += monitor.getTimes();
							}
						}
						n++;
					}
					i++;
				}
				putTimesList.add(putTimes);

				for (int k = 0; k < 24; k++) {
					int num = 0;
					for (int j : hourstamp) {
						if (k == j) {
							break;
						}
						if (num == hourstamp.size() - 1 && k != j) {
							try {
								putTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								putTimesList.add(0);
							}
						}
						num++;
					}
				}
			} else {
				int i = 0, putTimes = 0;
				for (int hour : hourstamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							putTimesList.add(putTimes);
							putTimes = 0;
						}
						if (monitor.getTime() == hour) {
							if (monitor.getType() == 2) {
								putTimes += monitor.getTimes();
							}
						}
						n++;
					}
					i++;
				}
				putTimesList.add(putTimes);
			}

			map.put("putTimes", putTimesList);

		} else if (times.equals("3")) {

			dateList = MonitorUtil.getDate(Integer.parseInt(times));

			map.put("dateList", dateList);

			timestamp = MonitorUtil.getByCount(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), bucketName, zoneId, companyId);

			if (timestamp.size() == 0 || timestamp == null) {
				for (int i = 0; i < 7; i++) {
					putTimesList.add(0);
				}
				map.put("putTimes", putTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// timestamp.add(timestamp.get(0));

			if (timestamp.size() != 7) {
				int i = 0, putTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							putTimesList.add(putTimes);
							putTimes = 0;
						}
						if (monitor.getDate().equals(str)) {
							if (monitor.getType() == 2) {
								putTimes += monitor.getTimes();
							}
						}
						n++;
					}
					i++;
				}
				putTimesList.add(putTimes);

				int k = 0;
				for (String string : dateList) {
					int num = 0;
					for (String j : timestamp) {
						if (string.equals(j.substring(j.indexOf("-") + 1))) {
							break;
						}
						if (num == timestamp.size() - 1 && !string.equals(j.substring(j.indexOf("-") + 1))) {
							try {
								putTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								putTimesList.add(0);
							}
						}
						num++;
					}
					k++;
				}
			} else {
				int i = 0, putTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							putTimesList.add(putTimes);
							putTimes = 0;
						}
						if (monitor.getDate().equals(str)) {
							if (monitor.getType() == 2) {
								putTimes += monitor.getTimes();
							}
						}
						n++;
					}
					i++;
				}
				putTimesList.add(putTimes);
			}

			map.put("putTimes", putTimesList);
		} else {
			dateList = MonitorUtil.getDate(Integer.parseInt(times));

			map.put("dateList", dateList);

			timestamp = MonitorUtil.getByCount(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), bucketName, zoneId, companyId);
			if (timestamp.size() == 0 || timestamp == null) {
				for (int i = 0; i < 30; i++) {
					putTimesList.add(0);
				}
				map.put("putTimes", putTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// timestamp.add(timestamp.get(0));

			if (timestamp.size() != 30) {
				int putTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							if (monitor.getType() == 2) {
								putTimes += monitor.getTimes();
							}
						}
						if (n == select.size() - 1) {
							putTimesList.add(putTimes);
							putTimes = 0;
						}
						n++;
					}
				}

				int k = 0;
				for (String string : dateList) {
					int num = 0;
					for (String j : timestamp) {
						if (string.equals(j.substring(j.indexOf("-") + 1))) {
							break;
						}
						if (num == timestamp.size() - 1 && !string.equals(j.substring(j.indexOf("-") + 1))) {
							try {
								putTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								putTimesList.add(0);
							}
						}
						num++;
					}
					k++;
				}
			} else {
				int putTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							if (monitor.getType() == 2) {
								putTimes += monitor.getTimes();
							}
						}
						if (n == select.size() - 1) {
							putTimesList.add(putTimes);
							putTimes = 0;
						}
						n++;
					}
				}
			}

			map.put("putTimes", putTimesList);
		}

		rd.setStatus(ExptNum.SUCCESS.getCode() + "");
		rd.setMsg(ExptNum.SUCCESS.getDesc());
		rd.setData(map);
		return JSONUtils.createObjectJson(rd);
	}

	/**
	 * 获得总请求次数(分别为今天、昨天、七天前、30天)
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "getMonitorAllTimes", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String getMonitorAllTimes(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, String> maps = ApiTool.getBodyString(request);
		String zoneId = maps.get("zoneId");
		String bucketName = maps.get("bucketName");
		String times = maps.get("times");

		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(times)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();

		String today = sdf.format(date);

		Calendar calendar1 = Calendar.getInstance();
		calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 1);
		Date today1 = calendar1.getTime();

		String yesterday = sdf.format(today1);

		List<OssMonitor> select = null;
		List<String> timestamp = null;
		List<Integer> hourstamp = null;

		// 总的请求次数
		List<Integer> allTimesList = new LinkedList<Integer>();

		Map<String, Object> map = new HashMap<String, Object>();

		String companyId = ApiTool.getUserMsg(request).getCompanyid();

		List<String> dateList = new LinkedList<String>();
		select = MonitorUtil.getByParam(ossMonitorService, ossBucketService, userCoreAccessService,
				Integer.parseInt(times), today, bucketName, zoneId, yesterday, companyId);
		if (times.equals("1") || times.equals("2")) {

			hourstamp = MonitorUtil.getByHour(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), today, bucketName, zoneId, yesterday, companyId);
			if (hourstamp.size() == 0 || hourstamp == null) {
				for (int i = 0; i < 24; i++) {
					allTimesList.add(0);
				}
				map.put("allTimes", allTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// hourstamp.add(hourstamp.get(0));
			if (hourstamp.size() != 24) {
				int i = 0, allTimes = 0;
				for (int hour : hourstamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (n == 0 && i != 0) {
							allTimesList.add(allTimes);
							allTimes = 0;
						}
						if (monitor.getTime() == hour) {
							allTimes += monitor.getTimes();
						}
						n++;
					}
					i++;
				}
				allTimesList.add(allTimes);

				for (int k = 0; k < 24; k++) {
					int num = 0;
					for (int j : hourstamp) {
						if (k == j) {
							break;
						}
						if (num == hourstamp.size() - 1 && k != j) {
							try {
								allTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								allTimesList.add(0);
							}
						}
						num++;
					}
				}

			} else {
				int allTimes = 0;
				for (int hour : hourstamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getTime() == hour) {
							allTimes += monitor.getTimes();
						}
						if (n == select.size() -1) {
							allTimesList.add(allTimes);
							allTimes = 0;
						}
						n++;
					}
				}
			}
			map.put("allTimes", allTimesList);
		} else if (times.equals("3")) {

			dateList = MonitorUtil.getDate(Integer.parseInt(times));

			map.put("dateList", dateList);

			timestamp = MonitorUtil.getByCount(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), bucketName, zoneId, companyId);

			if (timestamp.size() == 0 || timestamp == null) {
				for (int i = 0; i < 7; i++) {
					allTimesList.add(0);
				}
				map.put("allTimes", allTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// timestamp.add(timestamp.get(0));

			if (timestamp.size() != 7) {
				int allTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							allTimes += monitor.getTimes();
						}
						if (n == select.size() -1) {
							allTimesList.add(allTimes);
							allTimes = 0;
						}
						n++;
					}
				}
				
				int k = 0;
				for (String string : dateList) {
					int num = 0;
					for (String j : timestamp) {
						if (string.equals(j.substring(j.indexOf("-") + 1))) {
							break;
						}
						if (num == timestamp.size() - 1 && !string.equals(j.substring(j.indexOf("-") + 1))) {
							try {
								allTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								allTimesList.add(0);
							}
						}
						num++;
					}
					k++;
				}
			} else {
				int allTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							allTimes += monitor.getTimes();
						}
						if (n == select.size() -1) {
							allTimesList.add(allTimes);
							allTimes = 0;
						}
						n++;
					}
				}
			}
			map.put("allTimes", allTimesList);
		} else {
			dateList = MonitorUtil.getDate(Integer.parseInt(times));

			map.put("dateList", dateList);

			timestamp = MonitorUtil.getByCount(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), bucketName, zoneId, companyId);
			if (timestamp.size() == 0 || timestamp == null) {
				for (int i = 0; i < 30; i++) {
					allTimesList.add(0);
				}
				map.put("allTimes", allTimesList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// timestamp.add(timestamp.get(0));

			if (timestamp.size() != 30) {
				int allTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							allTimes += monitor.getTimes();
						}
						if (n == select.size() -1) {
							allTimesList.add(allTimes);
							allTimes = 0;
						}
						n++;
					}
				}

				int k = 0;
				for (String string : dateList) {
					int num = 0;
					for (String j : timestamp) {
						if (string.equals(j.substring(j.indexOf("-") + 1))) {
							break;
						}
						if (num == timestamp.size() - 1 && !string.equals(j.substring(j.indexOf("-") + 1))) {
							try {
								allTimesList.add(k, 0);
							} catch (Exception e) {
								log.error(e);
								allTimesList.add(0);
							}
						}
						num++;
					}
					k++;
				}
			} else {
				int allTimes = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							allTimes += monitor.getTimes();
						}
						if (n == select.size() -1) {
							allTimesList.add(allTimes);
							allTimes = 0;
						}
						n++;
					}
				}
			}

			map.put("allTimes", allTimesList);
		}

		rd.setStatus(ExptNum.SUCCESS.getCode() + "");
		rd.setMsg(ExptNum.SUCCESS.getDesc());
		rd.setData(map);
		return JSONUtils.createObjectJson(rd);

	}

	/**
	 * 获取下载流量(分别为今天、昨天、七天前、30天)
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "getMonitorFlow", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String getMonitorFlow(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Map<String, String> maps = ApiTool.getBodyString(request);
		String zoneId = maps.get("zoneId");
		String bucketName = maps.get("bucketName");
		String times = maps.get("times");

		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(times)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();

		String today = sdf.format(date);

		Calendar calendar1 = Calendar.getInstance();
		calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 1);
		Date today1 = calendar1.getTime();

		String yesterday = sdf.format(today1);

		List<OssMonitor> select = null;
		List<String> timestamp = null;
		List<Integer> hourstamp = null;

		Map<String, Object> map = new HashMap<String, Object>();

		// GET下载流量
		List<Long> getFlowList = new LinkedList<Long>();

		String companyId = ApiTool.getUserMsg(request).getCompanyid();

		List<String> dateList = new LinkedList<String>();

		select = MonitorUtil.getByParam(ossMonitorService, ossBucketService, userCoreAccessService,
				Integer.parseInt(times), today, bucketName, zoneId, yesterday, companyId);

		if (times.equals("1") || times.equals("2")) {
			hourstamp = MonitorUtil.getByHour(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), today, bucketName, zoneId, yesterday, companyId);

			if (hourstamp.size() == 0 || hourstamp == null) {
				for (int i = 0; i < 24; i++) {
					getFlowList.add((long) 0);
				}
				map.put("getFlow", getFlowList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// hourstamp.add(hourstamp.get(0));
			if (hourstamp.size() != 24) {
				long flowSize = 0;
				for (int hour : hourstamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getTime() == hour) {
							if(monitor.getType() == 1){
								flowSize += monitor.getSize();
							}
						}
						if (n == select.size() - 1) {
							getFlowList.add((long) flowSize);
							flowSize = 0;
						}
						n++;
					}
				}

				for (int k = 0; k < 24; k++) {
					int num = 0;
					for (int j : hourstamp) {
						if (k == j) {
							break;
						}
						if (num == hourstamp.size() - 1 && k != j) {
							try {
								getFlowList.add(k, (long) 0);
							} catch (Exception e) {
								log.error(e);
								getFlowList.add((long) 0);
							}
						}
						num++;
					}
				}
			} else {
				long flowSize = 0;
				for (int hour : hourstamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getTime() == hour) {
							if(monitor.getType() == 1){
								flowSize += monitor.getSize();
							}
						}
						if (n == select.size() - 1) {
							getFlowList.add((long) flowSize);
							flowSize = 0;
						}
						n++;
					}
				}
			}

			map.put("getFlow", getFlowList);
		} else if (times.equals("3")) {

			dateList = MonitorUtil.getDate(Integer.parseInt(times));

			map.put("dateList", dateList);

			timestamp = MonitorUtil.getByCount(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), bucketName, zoneId, companyId);

			if (timestamp.size() == 0 || timestamp == null) {
				for (int i = 0; i < 7; i++) {
					getFlowList.add((long) 0);
				}
				map.put("getFlow", getFlowList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// timestamp.add(timestamp.get(0));

			if (timestamp.size() != 7) {
				long flowSize = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							if(monitor.getType() == 1){
								flowSize += monitor.getSize();
							}
						}
						if (n == select.size() - 1) {
							getFlowList.add((long) flowSize);
							flowSize = 0;
						}
						n++;
					}
				}

				int k = 0;
				for (String string : dateList) {
					int num = 0;
					for (String j : timestamp) {
						if (string.equals(j.substring(j.indexOf("-") + 1))) {
							break;
						}
						if (num == timestamp.size() - 1 && !string.equals(j.substring(j.indexOf("-") + 1))) {
							try {
								getFlowList.add(k, (long) 0);
							} catch (Exception e) {
								log.error(e);
								getFlowList.add((long) 0);
							}
						}
						num++;
					}
					k++;
				}
			} else {
				long flowSize = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							if(monitor.getType() == 1){
								flowSize += monitor.getSize();
							}
						}
						if (n == select.size() - 1) {
							getFlowList.add((long) flowSize);
							flowSize = 0;
						}
						n++;
					}
				}
			}

			map.put("getFlow", getFlowList);
		} else {

			dateList = MonitorUtil.getDate(Integer.parseInt(times));

			map.put("dateList", dateList);

			timestamp = MonitorUtil.getByCount(ossMonitorService, ossBucketService, userCoreAccessService,
					Integer.parseInt(times), bucketName, zoneId, companyId);

			if (timestamp.size() == 0 || timestamp == null) {
				for (int i = 0; i < 30; i++) {
					getFlowList.add((long) 0);
				}
				map.put("getFlow", getFlowList);

				rd.setStatus(ExptNum.SUCCESS.getCode() + "");
				rd.setMsg(ExptNum.SUCCESS.getDesc());
				rd.setData(map);
				return JSONUtils.createObjectJson(rd);
			}

			// timestamp.add(timestamp.get(0));

			if (timestamp.size() != 30) {
				long flowSize = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							if(monitor.getType() == 1){
								flowSize += monitor.getSize();
							}
						}
						if (n == select.size() - 1) {
							getFlowList.add((long) flowSize);
							flowSize = 0;
						}
						n++;
					}
				}

				int k = 0;
				for (String string : dateList) {
					int num = 0;
					for (String j : timestamp) {
						
						if (string.equals(j.substring(j.indexOf("-") + 1))) {
							break;
						}
						if (num == timestamp.size() - 1 && !string.equals(j.substring(j.indexOf("-") + 1))) {
							try {
								getFlowList.add(k, (long) 0);
							} catch (Exception e) {
								log.error(e);
								getFlowList.add((long) 0);
							}
						}
						num++;
					}
					k++;
				}
			} else {
				long flowSize = 0;
				for (String str : timestamp) {
					int n = 0;
					for (OssMonitor monitor : select) {
						if (monitor.getDate().equals(str)) {
							if(monitor.getType() == 1){
								flowSize += monitor.getSize();
							}
						}
						if (n == select.size() -1) {
							getFlowList.add((long) flowSize);
							flowSize = 0;
						}
						n++;
					}
				}
			}

			map.put("getFlow", getFlowList);
		}

		rd.setStatus(ExptNum.SUCCESS.getCode() + "");
		rd.setMsg(ExptNum.SUCCESS.getDesc());
		rd.setData(map);
		return JSONUtils.createObjectJson(rd);
	}

	/**
	 * 获取总的请求次数和下载流量(最近一个月)
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "allMonitorTimes", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String allMonitorTimes(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, String> maps = ApiTool.getBodyString(request);
		String zoneId = maps.get("zoneId");
		String bucketName = maps.get("bucketName");

		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(zoneId)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}

		String companyId = ApiTool.getUserMsg(request).getCompanyid();
		Map<String, Object> map = new HashMap<String, Object>();

		if (!bucketName.equals("")) {
			List<OssBucket> select = ossBucketService.select(new OssBucket().setName(bucketName));
			try {
				if (select == null || select.size() == 0) {
					map.put("flowsize", 0);
					map.put("requesttimes", 0);
				} else {
					if (select.get(0).getFlowsize() == null || select.get(0).getRequesttimes() == null) {
						ossBucketService.updateByParam("flowSize='0',requestTimes='0' where name='" + bucketName + "'");
						map.put("flowsize", 0);
						map.put("requesttimes", 0);
					} else {
						map.put("flowsize", select.get(0).getFlowsize());
						map.put("requesttimes", select.get(0).getRequesttimes());
					}
				}
			} catch (Exception e) {
				log.error(e);
				map.put("flowsize", 0);
				map.put("requesttimes", 0);
			}

		} else {
			List<UserCoreAccess> select = userCoreAccessService.select(new UserCoreAccess().setCompanyid(companyId));
			try {
				if (select == null || select.size() == 0) {
					map.put("flowsize", 0);
					map.put("requesttimes", 0);
				} else {
					if (select.get(0).getFlowsize() == null || select.get(0).getRequesttimes() == null 
							|| select.get(0).getFlowsize().length() <= 3 || select.get(0).getRequesttimes().length() <= 3) {
						ossBucketService.updateByParam("flowSize='0',requestTimes='0' where name='" + bucketName + "'");
						map.put("flowsize", 0);
						map.put("requesttimes", 0);
					} else {
						String flowsize = select.get(0).getFlowsize();
						String requesttimes = select.get(0).getRequesttimes();
						flowsize.replaceAll("\"", "\'");
						requesttimes.replaceAll("\"", "\'");

						JSONObject flowsizeObject = JSONObject.fromObject(flowsize);
						JSONObject requesttimesObject = JSONObject.fromObject(requesttimes);

						map.put("flowsize", flowsizeObject.get(zoneId));
						map.put("requesttimes", requesttimesObject.get(zoneId));
					}
				}
			} catch (Exception e) {
				log.error(e);
				map.put("flowsize", 0);
				map.put("requesttimes", 0);
			}
		}
		rd.setStatus(ExptNum.SUCCESS.getCode() + "");
		rd.setMsg(ExptNum.SUCCESS.getDesc());
		rd.setData(map);
		return JSONUtils.createObjectJson(rd);
	}

	/**
	 * 监控数据
	 * @param request
	 * @param response
	 * @param zoneId
	 * @param companyId
	 * @param monitorType
	 *            1 小时 2 一周 3 一个月
	 * @return
	 */
	@RequestMapping(value = "getMonitor", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String getMonitor(HttpServletRequest request, HttpServletResponse response, String zoneId, String companyId,
			String monitorType, @RequestParam(required = false) String id) {

		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(zoneId, companyId, monitorType)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		int type = Integer.parseInt(monitorType);
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		List<CustomMonitorIndex> customMonitorIndex = null;
		if (id == null) {
			customMonitorIndex = customMonitorIndexService.selectByParam(null,
					"companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and productType='对象存储'");
		} else {
			customMonitorIndex = customMonitorIndexService.selectByParam(null, "companyId='" + companyId + "' and "
					+ "zoneId='" + zoneId + "' and productType='对象存储' and id='" + id + "'");
		}

		long putTimes = 0, getTimes = 0, postTimes = 0, deleteTiems = 0, getFlow = 0;
		double objectSize = 0.0;
		List<Map<String, Object>> mapList = new LinkedList<Map<String, Object>>();

		List<String> dateList = new ArrayList<String>();
		List<Object> putTimesList = new ArrayList<Object>();
		List<Object> getTimesList = new ArrayList<Object>();
		List<Object> postTimesList = new ArrayList<Object>();
		List<Object> deleteTimesList = new ArrayList<Object>();
		List<Object> getFlowList = new ArrayList<Object>();
		List<Object> objectSizeList = new ArrayList<Object>();

		if (type == 1) {

			Date date = new Date();
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {
				int bucketNum = 1;
				for (int i = 0; i < 24; i++) {
					dateList.add(i + ":" + "00");
				}
				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size() + 2];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5 + 2];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5 + 2];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5 + 2];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5 + 2];

				for (CustomMonitorIndex monitorIndex : customMonitorIndex) {

					String bucketNames = monitorIndex.getObjectstorageid();

					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());
					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {
						for (String bucketName : bucketNames.split(",")) {

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and date='"
											+ sdf.format(date) + "' and bucketName='" + bucketName + "'");

							List<Integer> hourList = ossStatisticsService.selectByHour("distinct(time)",
									" where companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and date='"
											+ sdf.format(date) + "' and bucketName='" + bucketName + "' ",
									"order by time asc");

							String strs = "";

							if (hourList.size() == 0 || hourList == null) {
								for (int i = 0; i < 24; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (hourList.size() != 24) {
								int i = 0;
								for (int hour : hourList) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (Integer.parseInt(monitor.getTime()) == hour) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								for (int k = 0; k < 24; k++) {
									int num = 0;
									for (int j : hourList) {
										if (k == j) {
											break;
										}
										if (num == hourList.size() - 1 && k != j) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
								}
							} else {
								int i = 0;
								for (int hour : hourList) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (Integer.parseInt(monitor.getTime()) == hour) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}
							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);

							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "today");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		} else if (type == 2) {

			dateList = MonitorUtil.getDate(3);
			
			Collections.reverse(dateList);
			
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {

				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size()];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5];

				int bucketNum = 1;
				for (CustomMonitorIndex monitorIndex : customMonitorIndex) {

					String bucketNames = monitorIndex.getObjectstorageid();
					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());

					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {

						for (String bucketName : bucketNames.split(",")) {

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where companyId='" + companyId + "' and zoneId='" + zoneId
											+ "' and date_sub(CURDATE(),INTERVAL 7 DAY) <= DATE(date) and bucketName='"
											+ bucketName + "' order by date asc");

							List<String> timestamp = ossStatisticsService.selectByCount("distinct(date)",
									" o join (SELECT id,str_to_date(date, '%Y-%c-%e') dates from oss_statistics) d on o.id = d.id where date_sub(CURDATE(),INTERVAL 7 DAY) <= DATE(d.dates) and o.bucketName= '"
											+ bucketName + "' and o.zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' ",
									"order by d.dates asc");

							String strs = "";

							if (timestamp.size() == 0 || timestamp == null) {
								for (int i = 0; i < 7; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (timestamp.size() != 7) {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								int k = 0;
								for (String string : dateList) {
									int num = 0;
									for (String j : timestamp) {
										if (string.equals(j.substring(j.indexOf("-") + 1))) {
											break;
										}
										if (num == timestamp.size() - 1
												&& !string.equals(j.substring(j.indexOf("-") + 1))) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
									k++;
								}

							} else {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}
							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);
							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "week");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		} else {
			dateList = MonitorUtil.getDate(4);

			Collections.reverse(dateList);
			
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {

				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size()];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5];

				int bucketNum = 1;
				for (CustomMonitorIndex monitorIndex : customMonitorIndex) {
					String bucketNames = monitorIndex.getObjectstorageid();

					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());
					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {
						for (String bucketName : bucketNames.split(",")) {

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where DATE_SUB(CURDATE(), INTERVAL 30 DAY) <= date(date) and bucketName = '"
											+ bucketName + "' and zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' order by date asc");

							List<String> timestamp = ossStatisticsService.selectByCount("distinct(date)",
									" o join (SELECT id,str_to_date(date, '%Y-%c-%e') dates from oss_statistics) d on o.id = d.id where DATE_SUB(CURDATE(), INTERVAL 30 DAY) <= date(d.dates) and o.bucketName = '"
											+ bucketName + "' and o.zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' ",
									"order by d.dates asc");

							String strs = "";

							if (timestamp.size() == 0 || timestamp == null) {
								for (int i = 0; i < 30; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (timestamp.size() != 30) {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								int k = 0;
								for (String string : dateList) {
									int num = 0;
									for (String j : timestamp) {
										if (string.equals(j.substring(j.indexOf("-") + 1))) {
											break;
										}
										if (num == timestamp.size() - 1
												&& !string.equals(j.substring(j.indexOf("-") + 1))) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
									k++;
								}
							} else {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}

							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);

							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "month");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		}
	}
	
	/**
	 * 监控数据
	 * @param request
	 * @param response
	 * @param zoneId
	 * @param companyId
	 * @param monitorType
	 *            1 小时 2 一周 3 一个月
	 * @return
	 */
	@RequestMapping(value = "getPandectbigMonitor", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String getPandectbigMonitor(HttpServletRequest request, HttpServletResponse response, String zoneId, String companyId,
			String monitorType, @RequestParam(required = false) String id) {

		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(zoneId, companyId, monitorType)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		int type = Integer.parseInt(monitorType);
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		List<PandectbigCustomMonitorIndex> customMonitorIndex = null;
		if (id == null) {
			customMonitorIndex = pandectbigCustomMonitorIndexService.selectByParam(null,
					"companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and productType='对象存储'");
		} else {
			customMonitorIndex = pandectbigCustomMonitorIndexService.selectByParam(null, "companyId='" + companyId + "' and "
					+ "zoneId='" + zoneId + "' and productType='对象存储' and id='" + id + "'");
		}

		long putTimes = 0, getTimes = 0, postTimes = 0, deleteTiems = 0, getFlow = 0;
		double objectSize = 0.0;
		List<Map<String, Object>> mapList = new LinkedList<Map<String, Object>>();

		List<String> dateList = new ArrayList<String>();
		List<Object> putTimesList = new ArrayList<Object>();
		List<Object> getTimesList = new ArrayList<Object>();
		List<Object> postTimesList = new ArrayList<Object>();
		List<Object> deleteTimesList = new ArrayList<Object>();
		List<Object> getFlowList = new ArrayList<Object>();
		List<Object> objectSizeList = new ArrayList<Object>();

		if (type == 1) {

			Date date = new Date();
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {
				int bucketNum = 1;
				for (int i = 0; i < 24; i++) {
					dateList.add(i + ":" + "00");
				}
				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size()];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5];

				for (PandectbigCustomMonitorIndex monitorIndex : customMonitorIndex) {

					String bucketNames = monitorIndex.getObjectstorageid();

					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());
					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {
						for (String bucketName : bucketNames.split(",")) {

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and date='"
											+ sdf.format(date) + "' and bucketName='" + bucketName + "'");

							List<Integer> hourList = ossStatisticsService.selectByHour("distinct(time)",
									" where companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and date='"
											+ sdf.format(date) + "' and bucketName='" + bucketName + "' ",
									"order by time asc");

							String strs = "";

							if (hourList.size() == 0 || hourList == null) {
								for (int i = 0; i < 24; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (hourList.size() != 24) {
								int i = 0;
								for (int hour : hourList) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (Integer.parseInt(monitor.getTime()) == hour) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								for (int k = 0; k < 24; k++) {
									int num = 0;
									for (int j : hourList) {
										if (k == j) {
											break;
										}
										if (num == hourList.size() - 1 && k != j) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
								}
							} else {
								int i = 0;
								for (int hour : hourList) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (Integer.parseInt(monitor.getTime()) == hour) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}
							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);

							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "today");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		} else if (type == 2) {

			dateList = MonitorUtil.getDate(3);
			
			Collections.reverse(dateList);
			
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {

				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size()];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5];

				int bucketNum = 1;
				for (PandectbigCustomMonitorIndex monitorIndex : customMonitorIndex) {

					String bucketNames = monitorIndex.getObjectstorageid();
					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());

					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {

						for (String bucketName : bucketNames.split(",")) {

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where companyId='" + companyId + "' and zoneId='" + zoneId
											+ "' and date_sub(CURDATE(),INTERVAL 7 DAY) <= DATE(date) and bucketName='"
											+ bucketName + "' order by date asc");

							List<String> timestamp = ossStatisticsService.selectByCount("distinct(date)",
									" o join (SELECT id,str_to_date(date, '%Y-%c-%e') dates from oss_statistics) d on o.id = d.id where date_sub(CURDATE(),INTERVAL 7 DAY) <= DATE(d.dates) and o.bucketName= '"
											+ bucketName + "' and o.zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' ",
									"order by d.dates asc");

							String strs = "";

							if (timestamp.size() == 0 || timestamp == null) {
								for (int i = 0; i < 7; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (timestamp.size() != 7) {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								int k = 0;
								for (String string : dateList) {
									int num = 0;
									for (String j : timestamp) {
										if (string.equals(j.substring(j.indexOf("-") + 1))) {
											break;
										}
										if (num == timestamp.size() - 1
												&& !string.equals(j.substring(j.indexOf("-") + 1))) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
									k++;
								}

							} else {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}
							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);
							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "week");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		} else {
			dateList = MonitorUtil.getDate(4);

			Collections.reverse(dateList);
			
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {

				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size()];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5];

				int bucketNum = 1;
				for (PandectbigCustomMonitorIndex monitorIndex : customMonitorIndex) {
					String bucketNames = monitorIndex.getObjectstorageid();

					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());
					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {
						for (String bucketName : bucketNames.split(",")) {

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where DATE_SUB(CURDATE(), INTERVAL 30 DAY) <= date(date) and bucketName = '"
											+ bucketName + "' and zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' order by date asc");

							List<String> timestamp = ossStatisticsService.selectByCount("distinct(date)",
									" o join (SELECT id,str_to_date(date, '%Y-%c-%e') dates from oss_statistics) d on o.id = d.id where DATE_SUB(CURDATE(), INTERVAL 30 DAY) <= date(d.dates) and o.bucketName = '"
											+ bucketName + "' and o.zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' ",
									"order by d.dates asc");

							String strs = "";

							if (timestamp.size() == 0 || timestamp == null) {
								for (int i = 0; i < 30; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (timestamp.size() != 30) {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								int k = 0;
								for (String string : dateList) {
									int num = 0;
									for (String j : timestamp) {
										if (string.equals(j.substring(j.indexOf("-") + 1))) {
											break;
										}
										if (num == timestamp.size() - 1
												&& !string.equals(j.substring(j.indexOf("-") + 1))) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
									k++;
								}
							} else {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}

							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);

							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "month");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		}
	}
	
	/**
	 * 监控数据
	 * @param request
	 * @param response
	 * @param zoneId
	 * @param companyId
	 * @param monitorType
	 *            1 小时 2 一周 3 一个月
	 * @return
	 */
	@RequestMapping(value = "getPandectMonitor", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String getPandectMonitor(HttpServletRequest request, HttpServletResponse response, String zoneId, String companyId,
			String monitorType, @RequestParam(required = false) String id) {

		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(zoneId, companyId, monitorType)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		int type = Integer.parseInt(monitorType);
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		List<PandectCustomMonitorIndex> customMonitorIndex = null;
		if (id == null) {
			customMonitorIndex = pandectCustomMonitorIndexService.selectByParam(null,
					"companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and productType='对象存储'");
		} else {
			customMonitorIndex = pandectCustomMonitorIndexService.selectByParam(null, "companyId='" + companyId + "' and "
					+ "zoneId='" + zoneId + "' and productType='对象存储' and id='" + id + "'");
		}

		long putTimes = 0, getTimes = 0, postTimes = 0, deleteTiems = 0, getFlow = 0;
		double objectSize = 0.0;
		List<Map<String, Object>> mapList = new LinkedList<Map<String, Object>>();

		List<String> dateList = new ArrayList<String>();
		List<Object> putTimesList = new ArrayList<Object>();
		List<Object> getTimesList = new ArrayList<Object>();
		List<Object> postTimesList = new ArrayList<Object>();
		List<Object> deleteTimesList = new ArrayList<Object>();
		List<Object> getFlowList = new ArrayList<Object>();
		List<Object> objectSizeList = new ArrayList<Object>();

		if (type == 1) {

			Date date = new Date();
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {
				int bucketNum = 1;
				for (int i = 0; i < 24; i++) {
					dateList.add(i + ":" + "00");
				}
				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size()];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5];

				for (PandectCustomMonitorIndex monitorIndex : customMonitorIndex) {

					String bucketNames = monitorIndex.getObjectstorageid();

					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());
					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {
						for (String bucketName : bucketNames.split(",")) {

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and date='"
											+ sdf.format(date) + "' and bucketName='" + bucketName + "'");

							List<Integer> hourList = ossStatisticsService.selectByHour("distinct(time)",
									" where companyId='" + companyId + "' and " + "zoneId='" + zoneId + "' and date='"
											+ sdf.format(date) + "' and bucketName='" + bucketName + "' ",
									"order by time asc");

							String strs = "";

							if (hourList.size() == 0 || hourList == null) {
								for (int i = 0; i < 24; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (hourList.size() != 24) {
								int i = 0;
								for (int hour : hourList) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (Integer.parseInt(monitor.getTime()) == hour) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								for (int k = 0; k < 24; k++) {
									int num = 0;
									for (int j : hourList) {
										if (k == j) {
											break;
										}
										if (num == hourList.size() - 1 && k != j) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
								}
							} else {
								int i = 0;
								for (int hour : hourList) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (Integer.parseInt(monitor.getTime()) == hour) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}
							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);

							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "today");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		} else if (type == 2) {

			dateList = MonitorUtil.getDate(3);
			
			Collections.reverse(dateList);
			
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {

				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size()];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5];

				int bucketNum = 1;
				for (PandectCustomMonitorIndex monitorIndex : customMonitorIndex) {

					String bucketNames = monitorIndex.getObjectstorageid();
					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());

					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {

						for (String bucketName : bucketNames.split(",")) {

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where companyId='" + companyId + "' and zoneId='" + zoneId
											+ "' and date_sub(CURDATE(),INTERVAL 7 DAY) <= DATE(date) and bucketName='"
											+ bucketName + "' order by date asc");

							List<String> timestamp = ossStatisticsService.selectByCount("distinct(date)",
									" o join (SELECT id,str_to_date(date, '%Y-%c-%e') dates from oss_statistics) d on o.id = d.id where date_sub(CURDATE(),INTERVAL 7 DAY) <= DATE(d.dates) and o.bucketName= '"
											+ bucketName + "' and o.zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' ",
									"order by d.dates asc");

							String strs = "";

							if (timestamp.size() == 0 || timestamp == null) {
								for (int i = 0; i < 7; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (timestamp.size() != 7) {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								int k = 0;
								for (String string : dateList) {
									int num = 0;
									for (String j : timestamp) {
										if (string.equals(j.substring(j.indexOf("-") + 1))) {
											break;
										}
										if (num == timestamp.size() - 1
												&& !string.equals(j.substring(j.indexOf("-") + 1))) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
									k++;
								}

							} else {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}
							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);
							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "week");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		} else {
			dateList = MonitorUtil.getDate(4);

			Collections.reverse(dateList);
			
			int a = 0;
			if (customMonitorIndex != null && customMonitorIndex.size() != 0) {

				List<Map<String, Object>>[] mapList2 = new List[customMonitorIndex.size()];
				Map<String, Object>[] mapStr2 = new Map[customMonitorIndex.size() * 5];
				Map<String, Object>[] mapStr = new Map[customMonitorIndex.size() * 5];
				MonitorL[] ml = new MonitorL[customMonitorIndex.size() * 5];
				List<Object>[] statistics = new List[customMonitorIndex.size() * 5];

				int bucketNum = 1;
				for (PandectCustomMonitorIndex monitorIndex : customMonitorIndex) {
					String bucketNames = monitorIndex.getObjectstorageid();

					mapList2[a] = new LinkedList<Map<String, Object>>();
					ml[bucketNum] = new MonitorL();

					ml[bucketNum].setName(monitorIndex.getIndexs());
					mapStr[a] = new LinkedHashMap<String, Object>();
					mapStr[a].put("name", ml[bucketNum].getName());
					if (bucketNames == null) {
						rd.setStatus(ExptNum.EMPTY.getCode() + "");
						rd.setMsg(ExptNum.EMPTY.getDesc());
						return JSONUtils.createObjectJson(rd);
					} else {
						for (String bucketName : bucketNames.split(",")) {

							ml[customMonitorIndex.size() + bucketNum] = new MonitorL();
							ml[customMonitorIndex.size() + bucketNum].setName(bucketName);

							mapStr2[bucketNum] = new LinkedHashMap<String, Object>();

							mapStr2[bucketNum].put("computerName", ml[customMonitorIndex.size() + bucketNum].getName());

							objectSizeList.clear();
							getFlowList.clear();
							getTimesList.clear();
							postTimesList.clear();
							putTimesList.clear();
							deleteTimesList.clear();

							List<OssStatistics> ossStatisticsList = ossStatisticsService.selectByParam(null,
									" where DATE_SUB(CURDATE(), INTERVAL 30 DAY) <= date(date) and bucketName = '"
											+ bucketName + "' and zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' order by date asc");

							List<String> timestamp = ossStatisticsService.selectByCount("distinct(date)",
									" o join (SELECT id,str_to_date(date, '%Y-%c-%e') dates from oss_statistics) d on o.id = d.id where DATE_SUB(CURDATE(), INTERVAL 30 DAY) <= date(d.dates) and o.bucketName = '"
											+ bucketName + "' and o.zoneId = '" + zoneId + "' AND companyId = '"
											+ companyId + "' ",
									"order by d.dates asc");

							String strs = "";

							if (timestamp.size() == 0 || timestamp == null) {
								for (int i = 0; i < 30; i++) {
									putTimesList.add((long) 0);
									getTimesList.add((long) 0);
									postTimesList.add((long) 0);
									deleteTimesList.add((long) 0);
									getFlowList.add((long) 0);
									objectSizeList.add((double) 0);
								}
								statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
								if ("capacity".equals(monitorIndex.getIndexs())) {
									for(Object object : objectSizeList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("flow".equals(monitorIndex.getIndexs())) {
									for(Object object : getFlowList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("gethttp".equals(monitorIndex.getIndexs())) {
									for(Object object : getTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("posthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : postTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("puthttp".equals(monitorIndex.getIndexs())) {
									for(Object object : putTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
									for(Object object : deleteTimesList){
										statistics[customMonitorIndex.size() + bucketNum].add(object);
									}
								}
								mapStr2[bucketNum].put("data",
										statistics[customMonitorIndex.size() + bucketNum]);
								mapList2[a].add(mapStr2[bucketNum]);
								bucketNum++;
								continue;
							}

							if (timestamp.size() != 30) {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;

								int k = 0;
								for (String string : dateList) {
									int num = 0;
									for (String j : timestamp) {
										if (string.equals(j.substring(j.indexOf("-") + 1))) {
											break;
										}
										if (num == timestamp.size() - 1
												&& !string.equals(j.substring(j.indexOf("-") + 1))) {
											try {
												putTimesList.add(k, (long) 0);
												getTimesList.add(k, (long) 0);
												postTimesList.add(k, (long) 0);
												deleteTimesList.add(k, (long) 0);
												getFlowList.add(k, (long) 0);
												objectSizeList.add(k, (double) 0);
											} catch (Exception e) {
												log.error(e);
												putTimesList.add((long) 0);
												getTimesList.add((long) 0);
												postTimesList.add((long) 0);
												deleteTimesList.add((long) 0);
												getFlowList.add((long) 0);
												objectSizeList.add((double) 0);
											}
										}
										num++;
									}
									k++;
								}
							} else {
								int i = 0;
								for (String hour : timestamp) {
									int n = 0;
									for (OssStatistics monitor : ossStatisticsList) {
										if (n == 0 && i != 0) {
											putTimesList.add((long) putTimes);
											getTimesList.add((long) getTimes);
											postTimesList.add((long) postTimes);
											deleteTimesList.add((long) deleteTiems);
											getFlowList.add((long) getFlow);
											objectSizeList.add((double) objectSize);
											putTimes = 0;
											getTimes = 0;
											postTimes = 0;
											deleteTiems = 0;
											getFlow = 0;
											objectSize = 0;
										}
										if (monitor.getDate().equals(hour)) {
											putTimes += monitor.getPuttimes();
											getTimes += monitor.getGettimes();
											postTimes += monitor.getPosttimes();
											deleteTiems += monitor.getDeletetimes();
											getFlow += monitor.getGetflows();
											objectSize += monitor.getObjectsize();
										}
										n++;
									}
									i++;
								}
								putTimesList.add((long) putTimes);
								getTimesList.add((long) getTimes);
								postTimesList.add((long) postTimes);
								deleteTimesList.add((long) deleteTiems);
								getFlowList.add((long) getFlow);
								objectSizeList.add((double) objectSize);
								putTimes = 0;
								getTimes = 0;
								postTimes = 0;
								deleteTiems = 0;
								getFlow = 0;
								objectSize = 0;
							}

							statistics[customMonitorIndex.size() + bucketNum] = new LinkedList<Object>();
							if ("capacity".equals(monitorIndex.getIndexs())) {
								for(Object object : objectSizeList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("flow".equals(monitorIndex.getIndexs())) {
								for(Object object : getFlowList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("gethttp".equals(monitorIndex.getIndexs())) {
								for(Object object : getTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("posthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : postTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("puthttp".equals(monitorIndex.getIndexs())) {
								for(Object object : putTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							} else if ("deletehttp".equals(monitorIndex.getIndexs())) {
								for(Object object : deleteTimesList){
									statistics[customMonitorIndex.size() + bucketNum].add(object);
								}
							}
							mapStr2[bucketNum].put("data",
									statistics[customMonitorIndex.size() + bucketNum]);

							mapList2[a].add(mapStr2[bucketNum]);

							bucketNum++;
						}
						mapStr[a].put("data", mapList2[a]);
						mapStr[a].put("x", dateList);
						mapStr[a].put("timeType", "month");
						mapStr[a].put("customMonitorIndex", monitorIndex);
						mapStr[a].put("mapType", "line");
						mapList.add(mapStr[a]);
						a++;
					}
				}
			} else {
				rd.setStatus(ExptNum.EMPTY.getCode() + "");
				rd.setMsg(ExptNum.EMPTY.getDesc());
				return JSONUtils.createObjectJson(rd);
			}

			map.put("list", mapList);

			rd.setStatus(ExptNum.SUCCESS.getCode() + "");
			rd.setMsg(ExptNum.SUCCESS.getDesc());
			rd.setData(map);
			return JSONUtils.createObjectJson(rd);
		}
	}

	/**
	 * 获取已使用的 流量与容量
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "getServiceCondition", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String getServiceCondition(HttpServletRequest request){
		
		RspData rd = new RspData();

		String companyId = ApiTool.getUserMsg(request).getCompanyid();
		
		List<UserTypeSource> typeSource = typeSourceService.selectByParam(null, "companyId='" + companyId + "'");
		
		int startFlux = typeSource.get(0).getMaxOssFreeFlux();
		int startStore = typeSource.get(0).getMaxossstore();
		
		List<UserCoreAccess> userCore = userCoreAccessService.selectByParam(null, "companyId='" + companyId + "'");
		startFlux += userCore.get(0).getFlowPackage();
		startStore += userCore.get(0).getCapacityPackage();
		
		List<String> monitorCount = ossMonitorService.selectByCount("count(size)", "companyId='" + companyId + "' and type=1" , null);
		
		List<OssBucket> putCount = ossBucketService.select(new OssBucket().setCompanyid(companyId));
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("startFlux", startFlux);
		map.put("startStore", startStore);
		map.put("monitorCount", monitorCount);
		map.put("putCount", putCount);
		
		rd.setStatus(ExptNum.SUCCESS.getCode() + "");
		rd.setMsg(ExptNum.SUCCESS.getDesc());
		rd.setData(map);
		return JSONUtils.createObjectJson(rd);
	}
	
	/**
	 * 获取已使用的 流量与容量
	 * @param request
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value = "getServiceConditionByZoneId", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public String getServiceConditionByZoneId(HttpServletRequest request) throws Exception{
		
		Map<String, String> maps = ApiTool.getBodyString(request);
		String zoneId = maps.get("zoneId");
		String companyId = ApiTool.getUserMsg(request).getCompanyid();
		
		RspData rd = new RspData();
		// 参数完整性判断
		if (!ParamIsNull.isNull(zoneId)) {
			rd.setStatus(GetResult.ERROR_STATUS + "");
			rd.setMsg(Config.REQUEST_Param_IS_NULL);
			return JSONUtils.createObjectJson(rd);
		}
		
		List<OssFlux> fluxList = fluxService.selectByParam(null, "companyId='" + companyId + "' and zoneId='" + zoneId + "'");
		
		for(OssFlux flux : fluxList){
			List<OssZone> ossZone = ossZoneService.select(new OssZone().setZoneid(flux.getZoneid()));
			flux.setZoneid(ossZone.get(0).getZonename());
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userFluxList", fluxList);
		
		rd.setStatus(ExptNum.SUCCESS.getCode() + "");
		rd.setMsg(ExptNum.SUCCESS.getDesc());
		rd.setData(map);
		return JSONUtils.createObjectJson(rd);
	}
}