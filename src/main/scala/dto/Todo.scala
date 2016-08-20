package dto

import org.joda.time.DateTime

case class Todo(
               id: Long,
               text: String,
               limitAt: DateTime,
               createdAt: DateTime,
               updatedAt: DateTime
               )
