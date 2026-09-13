package org.deeplauncher.network

import javafx.application.Platform
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.util.concurrent.ConcurrentHashMap

class DownloadEntry(val url: String, val fileName: String) {
    val progress = SimpleDoubleProperty(0.0)
    val status = SimpleStringProperty("queued")
}

object DownloadTracker {
    private val entriesByUrl = ConcurrentHashMap<String, DownloadEntry>()

    val entries: ObservableList<DownloadEntry> = FXCollections.observableArrayList()

    fun begin(url: String, fileName: String) {
        val entry = DownloadEntry(url, fileName)
        entriesByUrl[url] = entry
        Platform.runLater { entries.add(0, entry) }
    }

    fun update(url: String, writtenBytes: Long, totalBytes: Long) {
        val entry = entriesByUrl[url] ?: return
        entry.progress.set(if (totalBytes > 0) writtenBytes.toDouble() / totalBytes else 0.0)
    }

    fun end(url: String) {
        val entry = entriesByUrl.remove(url) ?: return
        entry.progress.set(1.0)
        entry.status.set("done")
    }

    fun fail(url: String, message: String) {
        val entry = entriesByUrl.remove(url) ?: return
        entry.status.set("failed · $message")
    }
}