package com.example.demo;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServerTest extends AbstractServerTest {

  /**
   * Тест на неизвестную команду.
   */
  @ParameterizedTest
  @ValueSource(strings = {
      "wrong_command",
      "another_one",
      "AnOTher Wrong COmmAND",
      "LIST_TASK",
      "some_user CREATE_TASK",
      "DELETE_TASK"
  })
  void shouldReturnWrongFormatForUnknownCommand(String command) {
    String response = sendMessage(command);
    assertEquals("WRONG_FORMAT", response);
  }


  /**
   * Тест на создание задач для пользователя.
   *
   * @param user  пользователь
   * @param tasks список задач
   */
  @ParameterizedTest
  @MethodSource("provideShouldCreateTaskSuccessfully")
  void shouldCreateTaskSuccessfully(String user, List<String> tasks) {
    for (String task : tasks) {
      assertEquals("CREATED", createTask(user, task));
    }
    assertListTask(user, tasks);
  }

  private static Stream<Arguments> provideShouldCreateTaskSuccessfully() {
    return Stream.of(
        arguments("USER1", List.of("MY_TASK1")),
        arguments("USER2", List.of("another_task1", "MY_TASK1")),
        arguments("user3", List.of("take_a_shower", "breakfast", "go_to_work"))
    );
  }

  /**
   * Тест на закрытие и удаление задачи пользователя.
   *
   * @param user пользователь
   * @param task задача
   */
  @ParameterizedTest
  @CsvSource({
      "User1,task1",
      "user2,task2",
      "another_user,another_task"
  })
  void shouldCloseAndDeleteTaskSuccessfully(String user, String task) {
    assertEquals("CREATED", createTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("CLOSED", closeTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("DELETED", deleteTask(user, task));
    assertListTask(user, emptyList());

    assertEquals("CREATED", createTask(user, task));
    assertListTask(user, List.of(task));
  }

  /**
   * Тест на повторное открытие закрытой задачи.
   *
   * @param user пользователь
   * @param task задача
   */
  @ParameterizedTest
  @CsvSource({
      "userA,taskA",
      "userB,taskB"
  })
  void shouldReopenExistingTask(String user, String task) {
    assertEquals("CREATED", createTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("CLOSED", closeTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("REOPENED", reopenTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("CLOSED", closeTask(user, task));
    assertListTask(user, List.of(task));
  }

  /**
   * Тест на то, что будут возвращены только задачи для указанного пользователя.
   */
  @Test
  void shouldReturnOnlyUsersTasks() {
    for (String task : List.of("task1", "task2")) {
      assertEquals("CREATED", createTask("user1", task));
    }
    for (String task : List.of("task56", "another_one", "a_perfect_circle")) {
      assertEquals("CREATED", createTask("user2", task));
    }
    assertListTask("user1", List.of("task1", "task2"));
    assertListTask("user2", List.of("task56", "another_one", "a_perfect_circle"));
  }

  /**
   * Тест на ошибку при попытке заново открыть задачу, которая еще не была закрыта.
   *
   * @param user пользователь
   * @param task задача
   */
  @ParameterizedTest
  @CsvSource({
      "someUser,anotherTask",
      "otherUser,otherTask"
  })
  void shouldFailToReopenTaskIfItIsNotClosed(String user, String task) {
    assertEquals("CREATED", createTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("ERROR", reopenTask(user, task));
    assertListTask(user, List.of(task));
  }

  /**
   * Тест на ошибку при попытке удалить задачу, если она еще не была закрыта.
   *
   * @param user пользователь
   * @param task задача
   */
  @ParameterizedTest
  @CsvSource({
      "user1,my_super_task",
      "user2,my_another_super_task"
  })
  void shouldFailToDeleteTaskIfItIsNotClosed(String user, String task) {
    assertEquals("CREATED", createTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("ERROR", deleteTask(user, task));
    assertListTask(user, List.of(task));
  }

  /**
   * Тест на ошибку при попытке создать задачу с названием, которое уже занято.
   *
   * @param user пользователь
   * @param task задача
   */
  @ParameterizedTest
  @CsvSource({
      "john,answer_the_phone",
      "jenny,complete_the_business_report"
  })
  void shouldFailToCreateTaskIfItAlreadyExists(String user, String task) {
    assertEquals("CREATED", createTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("ERROR", createTask(user, task));
    assertListTask(user, List.of(task));
  }

  /**
   * Тест на ошибку при попытке закрыть задачу другого пользователя.
   *
   * @param user пользователь
   * @param task задача
   */
  @ParameterizedTest
  @CsvSource({
      "billy_talent,rusted_from_the_rain",
      "kino,mama_i_know_we_are_all_deadly_sick"
  })
  void shouldFailIfUserHasNoRightsToCloseTask(String user, String task) {
    assertEquals("CREATED", createTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("ACCESS_DENIED", closeTask(user + "_IMPOSTER", task));
    assertListTask(user, List.of(task));
  }

  /**
   * Тест на ошибку при попытке удалить задачу другого пользователя.
   *
   * @param user пользователь
   * @param task задача
   */
  @ParameterizedTest
  @CsvSource({
      "queen,one_vision",
      "jackals,legacy"
  })
  void shouldFailIfUserHasNoRightsToDeleteTask(String user, String task) {
    assertEquals("CREATED", createTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("CLOSED", closeTask(user, task));
    assertListTask(user, List.of(task));

    assertEquals("ACCESS_DENIED", deleteTask(user + "_IMPOSTER", task));
    assertListTask(user, List.of(task));
  }
  
  //---------------------------------------------------------------------------------------------
  
  /**
   * Тест на ошибку при попытке переоткрыть задачу другого пользователя.
   *
   * @param user пользователь
   * @param task задача
   * @param user2 пользователь, пытающийся переоткрыть task
   */  
  @ParameterizedTest
  @CsvSource({
      "xxx, taskxxx, yyy",
      "123, task123, 321"
  })  
  
  void shouldFailIfUserHasNoRightsToReopenTask(String user, String task, String user2) {
	    assertEquals("CREATED", createTask(user, task));
	    assertListTask(user, List.of(task));

	    assertEquals("CLOSED", closeTask(user, task));
	    assertListTask(user, List.of(task));

	    assertEquals("ACCESS_DENIED", reopenTask(user2, task));
	    assertListTask(user, List.of(task));
	  }  
  
  /**
   * Тест на то, что пользователь может получить список задач другого пользователя
   * постановка это не запрещает
   *
   * @param user пользователь
   * @param task1 задача1
   * @param task2 задача2
   * @param task3 задача3
   * @param user2 пользователь, пытающийся получить список задач пользователя user
   */
  
  @ParameterizedTest
  @CsvSource({
      "u1, task_u1, task_u2, task_u3, other_u",
      "000, t000, t002, t003, 007",
      "xyz, abc, def, ghy, xyzxyz"
  })
  void shouldReturnOtherUsersTask(String user, String task1, String task2, String task3, String user2) {
	  assertEquals("CREATED", createTask(user, task1));
	  assertEquals("CREATED", createTask(user, task2));
	  assertEquals("CREATED", createTask(user, task3));
	  assertEquals("CLOSED", closeTask(user, task2));
	  
	  assertListTask(user, List.of(task1, task2, task3));
	  assertListTask(user2, List.of(task1, task2, task3), user);
  }
  
  //---------------------------------------------------------------------------------------------

  private String createTask(String user, String task) {
    return sendMessage(format("%s CREATE_TASK %s", user, task));
  }

  private String closeTask(String user, String task) {
    return sendMessage(format("%s CLOSE_TASK %s", user, task));
  }

  private String reopenTask(String user, String task) {
    return sendMessage(format("%s REOPEN_TASK %s", user, task));
  }

  private String deleteTask(String user, String task) {
    return sendMessage(format("%s DELETE_TASK %s", user, task));
  }

  private void assertListTask(String user, List<String> expectedTasks) {
    String listTaskResponse = sendMessage(user + " LIST_TASK " + user);
    assertEquals(format("TASKS %s", expectedTasks), listTaskResponse);
  } 
  
//---------------------------------------------------------------------------------------------
  private void assertListTask(String user, List<String> expectedTasks, String userWithTasks ) {
	    String listTaskResponse = sendMessage(user + " LIST_TASK " + userWithTasks);
	    assertEquals(format("TASKS %s", expectedTasks), listTaskResponse);
	  }  
}