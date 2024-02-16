/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthconnect.codelab.presentation.screen.inputreadings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission.Companion.getWritePermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Mass
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.data.dateTimeWithOffsetOrDefault
import com.example.healthconnect.codelab.presentation.theme.HealthConnectTheme
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import kotlin.time.Duration


@Composable
fun InputReadingsScreen(
  permissions: Set<String>,
  permissionsGranted: Boolean,
  readingsList: List<WeightRecord>,
  uiState: InputReadingsViewModel.UiState,
  onInsertClick: (Double) -> Unit = {},
  onError: (Throwable?) -> Unit = {},
  onPermissionsResult: () -> Unit = {},
  weeklyAvg: Mass?,
  onPermissionsLaunch: (Set<String>) -> Unit = {},
) {

  // Remember the last error ID, such that it is possible to avoid re-launching the error
  // notification for the same error when the screen is recomposed, or configuration changes etc.
  val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

  LaunchedEffect(uiState) {
    // If the initial data load has not taken place, attempt to load the data.
    if (uiState is InputReadingsViewModel.UiState.Uninitialized) {
      onPermissionsResult()
    }

    // The [InputReadingsScreenViewModel.UiState] provides details of whether the last action
    // was a success or resulted in an error. Where an error occurred, for example in reading
    // and writing to Health Connect, the user is notified, and where the error is one that can
    // be recovered from, an attempt to do so is made.
    if (uiState is InputReadingsViewModel.UiState.Error && errorId.value != uiState.uuid) {
      onError(uiState.exception)
      errorId.value = uiState.uuid
    }
  }

  var heartRateInput by remember { mutableStateOf("") }
  var dateTimeInput by remember { mutableStateOf("") }

  // Check if the input value is a valid weight
  fun hasValidDoubleInRange(weight: String): Boolean {
    val tempVal = weight.toDoubleOrNull()
    return if (tempVal == null) {
      false
    } else tempVal <= 1000
  }

  if (uiState != InputReadingsViewModel.UiState.Uninitialized) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (!permissionsGranted) {
        item {
          Button(
            onClick = { onPermissionsLaunch(permissions) }
          ) {
            Text(text = stringResource(R.string.permissions_button_label))
          }
        }
      } else {
        item {
          OutlinedTextField(
            value = heartRateInput,
            onValueChange = {
              heartRateInput = it
            },

            label = {
              Text("Heart Rate")
            },
            isError = !hasValidDoubleInRange(heartRateInput),
            keyboardActions = KeyboardActions { !hasValidDoubleInRange(heartRateInput) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
          if (!hasValidDoubleInRange(heartRateInput)) {
            Text(
              text = "Heartrate (1-300 bpm)",
              color = MaterialTheme.colors.error,
              style = MaterialTheme.typography.caption,
              modifier = Modifier.padding(start = 16.dp)
            )
          }

          Button(
            enabled = hasValidDoubleInRange(heartRateInput),
            onClick = {
              onInsertClick(heartRateInput.toDouble())
              // clear TextField when new weight is entered
              heartRateInput = ""
            },

            modifier = Modifier.fillMaxHeight()

          ) {
            Text(text = stringResource(id = R.string.save_readings_button))
          }

          Text(
            text = stringResource(id = R.string.heart_rate_history),
            fontSize = 24.sp,
            color = MaterialTheme.colors.primary
          )
        }
        items(readingsList) { reading ->
          Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // show local date and time
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            val zonedDateTime =
              dateTimeWithOffsetOrDefault(reading.time, reading.zoneOffset)
            Text(
              text = "${reading.weight}" + " ",
            )
            Text(text = formatter.format(zonedDateTime))
          }
        }
        item {
          Text(
            text = stringResource(id = R.string.weekly_avg), fontSize = 24.sp,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(vertical = 20.dp)
          )
          if (weeklyAvg == null) {
            Text(text = "0.0" + stringResource(id = R.string.beatperminute))
          } else {
            Text(text = "$weeklyAvg".take(5) + stringResource(id = R.string.beatperminute))
          }
        }
        item {
          Text("Assignment 2", fontSize = 24.sp,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(top = 20.dp)
          )
        }
        item {
          // Heart Rate input field
          OutlinedTextField(
            value = heartRateInput,
            onValueChange = {
              heartRateInput = it
            },
            label = {
              Text("Heartrate (1-300 bpm)")
            },
            isError = !heartRateInput.isDigitsOnly(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
          if (!heartRateInput.isDigitsOnly()) {
            Text(
              text = stringResource(id = R.string.valid_heart_rate_error_message),
              color = MaterialTheme.colors.error,
              style = MaterialTheme.typography.caption,
              modifier = Modifier.padding(start = 16.dp)
            )
          }

          // Date/Time input field
          OutlinedTextField(
            value = dateTimeInput,
            onValueChange = {
              dateTimeInput = it
            },
            label = {
              Text("Date/Time")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Load and Save buttons
          Row() {
            Button(
              onClick = {

              },
            ) {
              Text(text = stringResource(id = R.string.load_button))
            }

            Spacer(modifier = Modifier.width(20.dp))

            Button(
              enabled = hasValidDoubleInRange(heartRateInput) && heartRateInput.isDigitsOnly(),
              onClick = {
                onInsertClick(heartRateInput.toDouble())
                onInsertClick(dateTimeInput.toDouble())

                heartRateInput = ""
                dateTimeInput = ""
              },
            ) {
              Text(text = stringResource(id = R.string.save_readings_button))
            }
          }
        }
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(80.dp)
              .padding(vertical = 20.dp)
          ) {
            Text(
              text = stringResource(id = R.string.heart_rate_history),
              fontSize = 24.sp,
              color = MaterialTheme.colors.primary,
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
            )
          }
        }
        items(readingsList) { reading ->
          Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // show local date and time
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            val zonedDateTime =
              dateTimeWithOffsetOrDefault(reading.time, reading.zoneOffset)
            Text(
              text = "${reading.weight}" + " ",
            )
            Text(text = formatter.format(zonedDateTime))
            Spacer(modifier = Modifier.height(20.dp))
          }
        }


        item {
          Card(
            modifier = Modifier
              .width(300.dp)
              .heightIn(min = 56.dp)
          ) {
            Column(
              modifier = Modifier
                .padding(16.dp)
            ) {
              Text(text = "Camilo Meli", fontWeight = FontWeight.Bold)
              Text(text = "301240077", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun InputReadingsScreenPreview() {
  val inputTime = Instant.now()
  HealthConnectTheme(darkTheme = false) {

    InputReadingsScreen(
      permissions = setOf(),
      weeklyAvg = Mass.kilograms(54.5),
      permissionsGranted = true,
      readingsList = listOf(
        WeightRecord(
          weight = Mass.kilograms(54.0),
          time = inputTime,
          zoneOffset = null
        ),
        WeightRecord(
          weight = Mass.kilograms(55.0),
          time = inputTime,
          zoneOffset = null
        ),
      ),
      uiState = InputReadingsViewModel.UiState.Done
    )

  }
}




