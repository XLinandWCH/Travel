package com.xlin.travel.map

/**
 * 高德密钥下发接口约定的平台标识：ANDROID / IOS / WEB（与服务端 map 表 platform 列一致）。
 * 各端 actual 提供，服务端按该值查对应行。
 */
expect val mapPlatformKey: String
