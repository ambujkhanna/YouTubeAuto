package com.ambuj.youtubeauto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator

/**
 * Experimental Android for Cars Navigation service.
 *
 * This intentionally starts with a simple templated screen. The goal of this
 * branch is to verify whether a Navigation CarAppService remains available
 * when the DHU is switched to its simulated driving/restricted state.
 */
class ParkPlayCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Development/DHU testing only. Do not use for production builds.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        return ParkPlayNavigationSession()
    }
}

private class ParkPlayNavigationSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return ParkPlayNavigationScreen(carContext)
    }
}

private class ParkPlayNavigationScreen(
    carContext: CarContext
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val row = Row.Builder()
            .setTitle("ParkPlay Navigation Test")
            .addText("Experimental CarAppService")
            .build()

        val pane = Pane.Builder()
            .addRow(row)
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(
                Header.Builder()
                    .setStartHeaderAction(Action.APP_ICON)
                    .setTitle("ParkPlay")
                    .build()
            )
            .build()
    }
}
