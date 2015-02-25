package com.konka.dhtsearch.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.konka.dhtsearch.db.Transaction;
import com.konka.dhtsearch.db.TransactionJdbcImpl;
import com.konka.dhtsearch.db.exception.DhtException;
import com.konka.dhtsearch.db.jdbc.ConnectionProvider;
import com.konka.dhtsearch.db.models.DhtInfo;

public class DhtInfoDaoJdbcImpl implements DhtInfoDao {

	private static DhtInfoDao instance = new DhtInfoDaoJdbcImpl();

	public static DhtInfoDao getInstance() {
		return instance;
	}

	@Override
	public void insert(DhtInfo dhtinfo) throws DhtException {

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
			stmt.setString(1, dhtinfo.getInfo_hash());
			stmt.setString(2, dhtinfo.getInfo_hash());
			stmt.setString(3, dhtinfo.getInfo_hash());
			stmt.setString(4, dhtinfo.getInfo_hash());
			stmt.setString(5, dhtinfo.getInfo_hash());
			stmt.setString(6, dhtinfo.getInfo_hash());
			stmt.setString(7, dhtinfo.getInfo_hash());
			stmt.setString(8, dhtinfo.getInfo_hash());
			stmt.setString(9, dhtinfo.getInfo_hash());
			stmt.setString(10, dhtinfo.getInfo_hash());
			
			
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

			String query = "delete from persona where id = ?";
			PreparedStatement statement = conn.prepareStatement(query);
//			statement.setInt(1, persona.getId());
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
	public void update(DhtInfo dhtInfo) throws DhtException {
		try {
			String query = "update persona set nombre = ?, apellido = ?, edad = ? where id = ?";

			PreparedStatement statement = TransactionJdbcImpl.getInstance()
					.getConnection().prepareStatement(query);
//			statement.setString(1, persona.getNombre());
//			statement.setString(2, persona.getApellido());
//			statement.setInt(3, persona.getEdad());
//			statement.setInt(4, persona.getId());
			statement.executeUpdate();
		} catch (SQLException sqlException) {
			throw new DhtException(sqlException);
		}
	}

	public List<DhtInfo> findAll() throws DhtException {
		List<DhtInfo> lista = new LinkedList<DhtInfo>();
		try {
			String query = "select * from persona";
			PreparedStatement statement = ConnectionProvider.getInstance()
					.getConnection().prepareStatement(query);
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
			throw new IllegalArgumentException(
					"El id de persona no debe ser nulo");
		}
		DhtInfo persona = null;
		try {
			Connection c = ConnectionProvider.getInstance().getConnection();
			String query = "select * from persona where id = ?";
			PreparedStatement statement = c.prepareStatement(query);
			statement.setInt(1, idDhtInfo);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				persona = convertOne(resultSet);
			}
		} catch (SQLException sqlException) {
			throw new DhtException(sqlException);
		}
		return persona;
	}

	private DhtInfo convertOne(ResultSet resultSet) throws SQLException {
		DhtInfo retorno = new DhtInfo();

		retorno.setId(resultSet.getInt("id"));
//		retorno.setNombre(resultSet.getString("nombre"));
//		retorno.setApellido(resultSet.getString("apellido"));
//		retorno.setEdad(resultSet.getInt("edad"));

		return retorno;
	}

}
