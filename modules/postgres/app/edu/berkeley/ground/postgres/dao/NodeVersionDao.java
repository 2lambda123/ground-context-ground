package edu.berkeley.ground.postgres.dao;

import edu.berkeley.ground.lib.exception.GroundException;
import edu.berkeley.ground.lib.factory.core.NodeVersionFactory;
import edu.berkeley.ground.postgres.dao.VersionSuccessorDao;
import edu.berkeley.ground.postgres.dao.VersionHistoryDagDao;
import edu.berkeley.ground.postgres.dao.TagDao;
import edu.berkeley.ground.lib.model.core.NodeVersion;
import edu.berkeley.ground.postgres.utils.PostgresUtils;
import java.util.ArrayList;
import java.util.List;
import play.db.Database;
import javax.sql.DataSource;
import edu.berkeley.ground.lib.utils.IdGenerator;
import edu.berkeley.ground.postgres.utils.PostgresUtils;
import edu.berkeley.ground.postgres.utils.PostgresClient;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import play.db.Database;
import play.libs.Json;

public class NodeVersionDao extends RichVersionDao<NodeVersion> implements NodeVersionFactory {

  public final void create(final Database dbSource, final PostgresClient dbClient, final NodeVersion nodeVersion, IdGenerator idGenerator, List<Long> parentIds)
      throws GroundException {
    final List<String> sqlList = new ArrayList<>();

    //Call super.create to ensure that versions exist
    final long uniqueId = idGenerator.generateVersionId();
    NodeVersion newNodeVersion = new NodeVersion(uniqueId, nodeVersion.getTags(), nodeVersion.getStructureVersionId(),
      nodeVersion.getReference(), nodeVersion.getParameters(), nodeVersion.getNodeId());
    //TODO create version successor
    //TODO pass PostgresClient and VHDD and TagFactory to ItemDao constructor
    VersionSuccessorDao versionSuccessorDao = new VersionSuccessorDao(dbClient, idGenerator);
    VersionHistoryDagDao versionHistoryDagDao = new VersionHistoryDagDao(dbClient, versionSuccessorDao);
    new NodeDao(dbClient, versionHistoryDagDao, new TagDao()).update(nodeVersion.getNodeId(), nodeVersion.getId(), parentIds);
    //Call super to create 1.version, 2. structure version (need to create a node_id)?, 3. rich version, 4. node_version
    try {
      sqlList.addAll(super.createSqlList(dbSource, newNodeVersion));
      sqlList.add(
        String.format(
          "insert into node_version (id, node_id) values (%s,%s)",
          uniqueId, nodeVersion.getNodeId()));
    PostgresUtils.executeSqlList(dbSource, sqlList);
      PostgresUtils.executeSqlList(dbSource, sqlList);
    } catch (Exception e) {
      throw new GroundException(e);
    }
  }

  @Override
  public NodeVersion retrieveFromDatabase(Database dbSource, long id) throws GroundException {
    String sql = String.format("select * from node_version where id=%d", id);
    JsonNode json = Json.parse(PostgresUtils.executeQueryToJson(dbSource, sql));
    return Json.fromJson(json, NodeVersion.class);
  }
}
