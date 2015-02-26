package com.konka.dhtsearch.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.konka.dhtsearch.db.exception.DhtException;
import com.konka.dhtsearch.db.jdbc.ConnectionProvider;
import com.konka.dhtsearch.db.models.DhtInfo;
import com.konka.dhtsearch.db.transaction.Transaction;
import com.konka.dhtsearch.db.transaction.TransactionJdbcImpl;

public class DhtInfoDaoJdbcImpl implements DhtInfoDao {

	private static DhtInfoDao instance = new DhtInfoDaoJdbcImpl();

	public static DhtInfoDao getInstance() {
		return instance;
	}

	@Override
	public synchronized void insert(DhtInfo dhtinfo) throws DhtException {

		Transaction tx = TransactionJdbcImpl.getInstance();
		Connection conn = tx.getConnection();

		try {
			tx.begin();
			String sql = "insert into dhtinfo " + //
					" (info_hash,peerIp,fileName,"//
					+ "torrentFilePath,filesize,"//
					+ "createTime,fileList,lastRequestsTime,"//
					+ "analysised,tag)" + //
					" values (?,?,?,?,?,?,?,?,?,?)";
			PreparedStatement stmt = conn.prepareStatement(sql);

			// fill the values
			stmt.setString(1, dhtinfo.getInfo_hash());// info_hash
			stmt.setString(2, dhtinfo.getPeerIp());// peerIp
			stmt.setNull(3, java.sql.Types.VARCHAR);// fileName
			stmt.setNull(4, java.sql.Types.VARCHAR);// torrentFilePath
			stmt.setNull(5, java.sql.Types.VARCHAR);// filesize
			stmt.setNull(6, java.sql.Types.TIMESTAMP);// createTime
			stmt.setNull(7, java.sql.Types.VARCHAR);// fileList
			stmt.setNull(8, java.sql.Types.VARCHAR);// lastRequestsTime
			stmt.setInt(9, dhtinfo.getAnalysised());// analysised
			stmt.setString(10, dhtinfo.getTag());// tag

			stmt.executeUpdate();

			tx.commit();
		} catch (SQLException sqlException) {
			throw new DhtException(sqlException);
		} finally {
			try {
				conn.close();
			} catch (SQLException sqlException) {
				throw new DhtException(sqlException);
			}
		}
	}

	@Override
	public void delete(DhtInfo dhtInfo) throws DhtException {
		Transaction tx = TransactionJdbcImpl.getInstance();
		Connection conn = tx.getConnection();

		try {
			tx.begin();

			String query = "delete from dhtinfo where id = ?";
			PreparedStatement statement = conn.prepareStatement(query);
			// statement.setInt(1, dhtinfo.getId());
			statement.executeUpdate();

			tx.commit();

		} catch (SQLException sqlException) {
			throw new DhtException(sqlException);
		} finally {
			try {
				conn.close();
			} catch (SQLException sqlException) {
				throw new DhtException(sqlException);
			}
		}
	}

	@Override
	public void update(DhtInfo dhtinfo) throws DhtException {
		try {
			String query = "update dhtinfo set "//
					+ "fileName = ?, " + "filesize = ?, "//
					+ "createTime = ? " + ",analysised = ? " + ",fileList = ? "//
					+ " where id = ?";

			PreparedStatement statement = TransactionJdbcImpl.getInstance().getConnection().prepareStatement(query);
			// statement.setString(1, "蔡庆和");
			// statement.sets
			statement.setString(1, dhtinfo.getFileName());
			statement.setLong(2, dhtinfo.getFileSize());
			statement.setLong(3, dhtinfo.getCreateTime());
			statement.setInt(4, dhtinfo.getAnalysised());
			String filelist = dhtinfo.getFileList();
			statement.setString(5, filelist == null ? "" : filelist);
			statement.setLong(6, dhtinfo.getId());

			statement.executeUpdate();
		} catch (SQLException sqlException) {
			throw new DhtException(sqlException);
		}
	}

	public List<DhtInfo> findAll() throws DhtException {
		List<DhtInfo> lista = new LinkedList<DhtInfo>();
		try {
			String query = "select * from dhtinfo";
			PreparedStatement statement = ConnectionProvider.getInstance().getConnection().prepareStatement(query);
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next()) {
				lista.add(convertOne(resultSet));
			}
		} catch (SQLException sqlException) {
			throw new DhtException(sqlException);
		}
		return lista;
	}

	@Override
	public DhtInfo findById(Integer idDhtInfo) throws DhtException {
		if (idDhtInfo == null) {
			throw new IllegalArgumentException("El id de dhtinfo no debe ser nulo");
		}
		DhtInfo dhtinfo = null;
		try {
			Connection c = ConnectionProvider.getInstance().getConnection();
			String query = "select * from dhtinfo where id = ?";
			PreparedStatement statement = c.prepareStatement(query);
			statement.setInt(1, idDhtInfo);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				dhtinfo = convertOne(resultSet);
			}
		} catch (SQLException sqlException) {
			throw new DhtException(sqlException);
		}
		return dhtinfo;
	}

	/**
	 * 根据条件查找
	 * 
	 * @param where
	 * @return
	 * @throws DhtException
	 */
	public List<DhtInfo> findByWhere(Map<String, String> where, int limit) throws DhtException {
		List<DhtInfo> lista = new LinkedList<DhtInfo>();
		try {
			String query = "select * from dhtinfo where 1 = 1 ";
			StringBuilder querySql = new StringBuilder(query);

			for (String key : where.keySet()) {
				String value = where.get(key);
				querySql.append(" and " + key + " = " + value);
			}
			querySql.append(" order by id ");
			querySql.append(" limit 0 , " + limit);
			PreparedStatement statement = ConnectionProvider.getInstance().getConnection().prepareStatement(querySql.toString());
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next()) {
				lista.add(convertOne(resultSet));
			}
		} catch (SQLException sqlException) {
			throw new DhtException(sqlException);
		}
		return lista;
	}

	/**
	 * 查找没有解析的info_hash;
	 * 
	 * @param limit
	 *            要返回的条数
	 * @return
	 * @throws DhtException
	 */
	public List<DhtInfo> getNoAnalyticDhtInfos(int count) throws DhtException {
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("analysised", "0");
		return findByWhere(hashMap, count);
	}

	private DhtInfo convertOne(ResultSet resultSet) throws SQLException {
		DhtInfo dhtInfo = new DhtInfo();

		dhtInfo.setId(resultSet.getInt("id"));
		dhtInfo.setInfo_hash(resultSet.getString("info_hash"));
		dhtInfo.setAnalysised(resultSet.getInt("analysised"));

		return dhtInfo;
	}

}
