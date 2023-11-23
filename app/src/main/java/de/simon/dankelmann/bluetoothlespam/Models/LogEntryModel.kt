package de.simon.dankelmann.bluetoothlespam.Models

import de.simon.dankelmann.bluetoothlespam.Constants.LogLevel
import java.io.Serializable

class LogEntryModel : Serializable {
    var message:String = ""
    var level: LogLevel = LogLevel.Info
}