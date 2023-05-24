# Tasks Management Service API Documentation

## Endpoints

### GET /tasks
Returns a list of tasks based on the provided query parameters.

Query Parameters

- status (optional): Filters tasks by status. Possible values: NOT_STARTED, IN_PROGRESS, COMPLETED.
- severity (optional): Filters tasks by severity. Possible values: LOW, MEDIUM, HIGH, URGENT.
- owner (optional): Filters tasks by owner.
- order (optional): Specifies the order in which the tasks should be sorted. Possible values: asc, desc.

**Response**

A list of Task objects in JSON format, filtered and sorted based on the provided query parameters.

---

### `GET /tasks/{id}`

Returns a task by its ID.

**Parameters**

`id` - The ID of the task.

**Response**

A Task object in JSON format if the task exists. If not, an error message will be returned.

---

### `POST /tasks`

Creates a new task.

**Body**

A Task object in JSON format.

**Response**

A Task object in JSON format with the `taskId` field filled in. If the provided task is invalid (e.g. if the due date is in the past), an error message will be returned.

---

### `PUT /tasks/{id}`

Updates an existing task.

**Parameters**

`id` - The ID of the task to update.

**Body**

A Task object in JSON format.

**Response**

The updated Task object in JSON format. If the provided ID or task is invalid, an error message will be returned.

---

### `DELETE /tasks/{id}`

Deletes a task.

**Parameters**

`id` - The ID of the task to delete.

**Response**

A confirmation message that the task was successfully deleted. If the task does not exist, an error message will be returned.

---

## Task Object

A task object has the following structure:

```json
{
    "taskId": 1,
    "title": "Title",
    "description": "Description",
    "status": "Status",
    "severity": "Severity",
    "owner": "Owner",
    "dueDate": "2023-12-31T23:59:59Z"
}
```
---

## Field descriptions:

- taskId: The ID of the task. This field is automatically filled in by the server when creating a new task.
- title: The title of the task.
- description: The description of the task.
- status: The status of the task.
- severity: The severity of the task.
- owner: The owner of the task.
- dueDate: The due date of the task, in ISO 8601 format.

---


## Errors
Errors are returned in the following format:

```json
{
    "error": "Error message"
}
```