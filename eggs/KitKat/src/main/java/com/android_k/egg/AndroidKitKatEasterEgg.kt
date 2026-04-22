package com.android_k.egg

import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.dede.basic.provider.Component
import com.dede.basic.provider.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import kotlin.ranges.IntRange

@Module
@InstallIn(SingletonComponent::class)
class AndroidKitKatEasterEgg : EasterEggProvider, ComponentProvider {

    @Provides
    @IntoSet
    @Singleton
    override fun provideEasterEgg(): BaseEasterEgg {
        return EasterEggGroup(
            object : EasterEgg(
                R.drawable.k_android_logo,
                R.string.k_dessert_case,
                R.string.k_android_nickname,
                IntRange(Build.VERSION_CODES.KITKAT, Build.VERSION_CODES.KITKAT_WATCH),
                PlatLogoActivity::class.java
            ) {
                override fun provideSnapshotProvider(): SnapshotProvider {
                    return com.android_k.egg.SnapshotProvider()
                }
            },
            object : EasterEgg(
                R.drawable.k_platlogo_preview,
                R.string.k_key_lime_pie,
                R.string.k_preview_nickname,
                EasterEggProviderKt.toRange(Build.VERSION_CODES.KITKAT),
                com.android_k.egg.preview.PlatLogoActivity::class.java,
                EasterEggProviderKt.toRange(EasterEgg.VERSION_CODES_FULL.K_PREVIEW)
            ) {
                override fun provideSnapshotProvider(): com.android_k.egg.preview.SnapshotProvider {
                    return com.android_k.egg.preview.SnapshotProvider()
                }
            }
        )
    }

    @Provides
    @IntoSet
    @Singleton
    override fun provideComponent(): Component {
        return object : Component(
            R.drawable.k_platlogo,
            R.string.k_dessert_case,
            R.string.k_android_nickname,
            IntRange(Build.VERSION_CODES.KITKAT, Build.VERSION_CODES.KITKAT_WATCH)
        ) {
            override fun isSupported(): Boolean = true

            override fun isEnabled(context: Context): Boolean {
                val cn = ComponentName(context, DessertCaseDream::class.java)
                return Component.isEnabled(cn, context)
            }

            override fun setEnabled(context: Context, enable: Boolean) {
                val cn = ComponentName(context, DessertCaseDream::class.java)
                Component.setEnable(cn, context, enable)
            }
        }
    }

    @Provides
    @IntoSet
    @Singleton
    override fun provideTimelineEvents(): List<TimelineEvent> {

        val e2 = TimelineEvent.timelineEvent(
            Build.VERSION_CODES.KITKAT,
            "K.\nReleased publicly as Android 4.4 in October 2013."
        ).apply {
            setAndroidLogo(R.drawable.k_android_logo_2014_2015)
        }

        return listOf(
            TimelineEvent.timelineEvent(
                Build.VERSION_CODES.KITKAT_WATCH,
                "K for watches.\nReleased publicly as Android 4.4W in June 2014."
            ),
            e2
        )
    }
}
