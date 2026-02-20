package com.merak.core.installer

object ThemeModuleProvider {

    val installModules = listOf(
        ThemeModule("系统UI (Framework)", ThemeFlags.FRAMEWORK, isDefaultChecked = true),
        ThemeModule("桌面外观 (Launcher)", ThemeFlags.LAUNCHER, isDefaultChecked = true),
        ThemeModule("状态栏 (Status Bar)", ThemeFlags.STATUSBAR, isDefaultChecked = true),
        ThemeModule("桌面图标 (Icons)", ThemeFlags.ICONS, isDefaultChecked = true),
        ThemeModule("锁屏样式 (Lockstyle)", ThemeFlags.LOCKSTYLE, isDefaultChecked = true),
        ThemeModule("桌面壁纸 (Wallpaper)", ThemeFlags.WALLPAPER, isDefaultChecked = true),
        ThemeModule("锁屏壁纸 (Lockscreen)", ThemeFlags.LOCKSCREEN, isDefaultChecked = true),
        ThemeModule("联系人/拨号 (Contacts)", ThemeFlags.CONTACT, isDefaultChecked = true),
        ThemeModule("短信 (MMS)", ThemeFlags.MMS, isDefaultChecked = true),
        ThemeModule("全局字体 (Fonts)", ThemeFlags.FONTS, isDefaultChecked = false),
        ThemeModule("来电铃声 (Ringtone)", ThemeFlags.RINGTONE, isDefaultChecked = false),
        ThemeModule("通知音 (Notification)", ThemeFlags.NOTIFICATION, isDefaultChecked = false),
        ThemeModule("闹钟铃声 (Alarm)", ThemeFlags.ALARM, isDefaultChecked = false),
        ThemeModule("系统音效 (Audio Effect)", ThemeFlags.AUDIO_EFFECT, isDefaultChecked = false),
        ThemeModule("开机动画 (Boot Anim)", ThemeFlags.BOOT_ANIMATION, isDefaultChecked = false),
        ThemeModule("开机声音 (Boot Audio)", ThemeFlags.BOOT_AUDIO, isDefaultChecked = false),
        ThemeModule("动态壁纸 (Live Wallpaper)", ThemeFlags.MI_WALLPAPER, isDefaultChecked = false)
    )
}