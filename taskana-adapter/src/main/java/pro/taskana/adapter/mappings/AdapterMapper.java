package pro.taskana.adapter.mappings;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import pro.taskana.adapter.scheduler.AgentType;

/**
 * Mapper for the Timestamps of the last creation of Taskana tasks or completion of referenced tasks.
 *
 * @author kkl
 */
@Mapper
public interface AdapterMapper {

    @Select("<script>"
        + "SELECT MAX(QUERY_TIMESTAMP) "
        + "FROM QUERY_HISTORY "
        + "WHERE SYSTEM_URL = #{systemUrl} AND AGENT_TYPE = #{agentType} "
        + "</script>")
    Instant getLatestQueryTimestamp(@Param("systemUrl") String systemUrl, @Param("agentType") AgentType agentType);

    @Select("<script>"
        + "SELECT MAX(CREATED) "
        + "FROM TASKS "
        + "WHERE SYSTEM_URL = #{systemUrl}"
        + "</script>")
    Instant getYoungestTaskCreationTimestamp(@Param("systemUrl") String systemUrl);

    @Select("<script>"
        + "SELECT MIN(CREATED) "
        + "FROM TASKS "
        + "WHERE SYSTEM_URL = #{systemUrl}"
        + "</script>")
    Instant getOldestTaskCreationTimestamp(@Param("systemUrl") String systemUrl);

    @Select("<script>"
        + "SELECT ID "
        + "FROM TASKS "
        + "WHERE (SYSTEM_URL = #{systemUrl} "
        + "AND  ID IN (<foreach item='item' collection='taskIdsIn' separator=',' >#{item}</foreach>))"
        + "</script>")
    @Results(value = {@Result(property = "taskId", column = "ID")})
    List<String> findExistingTaskIds(@Param("systemUrl") String systemUrl, @Param("taskIdsIn") List<String> taskIdsIn);

    @Select("<script>"
        + "SELECT ID "
        + "FROM TASKS "
        + "WHERE ID IN (<foreach item='item' collection='taskIdsIn' separator=',' >#{item}</foreach>) "
        + "AND NOT (COMPLETED IS NULL) "
        + "</script>")
    @Results(value = {@Result(property = "taskId", column = "ID")})
    List<String> findAlreadyCompletedTaskIds(@Param("taskIdsIn") List<String> taskIdsIn);

    @Insert("INSERT INTO TASKS (ID, CREATED, SYSTEM_URL) VALUES (#{id}, #{created}, #{systemUrl})")
    void registerCreatedTask(@Param("id") String id,
        @Param("created") Instant created,
        @Param("systemUrl") String systemUrl);

    @Insert("INSERT INTO QUERY_HISTORY (ID, QUERY_TIMESTAMP, SYSTEM_URL, AGENT_TYPE) "
        + "VALUES (#{id}, #{queryTimestamp}, #{systemUrl}, #{agentType})")
    void rememberLastQueryTime(@Param("id") String id,
        @Param("queryTimestamp") Instant queryTimestamp,
        @Param("systemUrl") String systemUrl,
        @Param("agentType") AgentType agentType);

    @Select("<script>SELECT MAX(COMPLETED) FROM TASKS </script>")
    Instant getLatestCompletedTimestamp();

    @Update("UPDATE TASKS SET COMPLETED = #{completed} where ID = #{id}")
    void registerTaskCompleted(@Param("id") String id,
        @Param("completed") Instant completed);

    @Delete(value = "DELETE FROM TASKS WHERE COMPLETED < #{completedBefore}")
    void cleanupTasksCompletedBefore(@Param("completedBefore") Instant completedBefore);

    @Delete(value = "DELETE FROM QUERY_HISTORY where QUERY_TIMESTAMP < #{queriedBefore}")
    void cleanupQueryHistoryEntries(@Param("queriedBefore") Instant queriedBefore);

    @Select("<script>"
        + "SELECT ID "
        + "FROM TASKS "
        + "WHERE ID IN (<foreach item='item' collection='taskIdsIn' separator=',' >#{item}</foreach>) "
        + "AND (COMPLETED IS NULL) "
        + "</script>")
    @Results(value = {@Result(property = "taskId", column = "ID")})
    List<String> findActiveTasks(@Param("systemUrl") String systemUrl, @Param("taskIdsIn") List<String> taskIdsIn);

}
