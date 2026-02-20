package com.merak.core.installer

object ThemeFlags {
    const val FRAMEWORK = 0x1L         // 系统框架
    const val WALLPAPER = 0x2L         // 桌面壁纸
    const val LOCKSCREEN = 0x4L        // 锁屏壁纸
    const val ICONS = 0x8L             // 桌面图标
    const val FONTS = 0x10L            // 全局字体
    const val BOOT_ANIMATION = 0x20L   // 开机动画
    const val BOOT_AUDIO = 0x40L       // 开机声音
    const val MMS = 0x80L              // 短信
    const val RINGTONE = 0x100L        // 来电铃声
    const val NOTIFICATION = 0x200L    // 通知音
    const val ALARM = 0x400L           // 闹钟铃声
    const val CONTACT = 0x800L         // 联系人/拨号
    const val LOCKSTYLE = 0x1000L      // 锁屏样式
    const val STATUSBAR = 0x2000L      // 状态栏
    const val LAUNCHER = 0x4000L       // 桌面外观
    const val AUDIO_EFFECT = 0x8000L   // 系统音效
    const val MI_WALLPAPER = 0x80000L  // 动态/超级壁纸
    const val VIDEO_WALLPAPER = 0x10000000L // 视频壁纸

    const val ALL = -1L                // 全部应用
}