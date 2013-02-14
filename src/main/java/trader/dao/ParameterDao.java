package trader.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.stereotype.Repository;

import trader.domain.Parameter;

@Repository
public class ParameterDao extends NamedParameterJdbcDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired
    public ParameterDao(DataSource dataSource) {
    	setDataSource(dataSource);
    }

    public List<Parameter> getParameters() {
        logger.info("Getting parameters");
        List<Parameter> parameters = getNamedParameterJdbcTemplate().query(
        		" SELECT parameter_cd," +
        		"        parameter_value" +
    			" FROM   parameter", 
    			new MapSqlParameterSource(),
    			new ParameterMapper());
        return parameters;
    }
  
    private static class ParameterMapper implements ParameterizedRowMapper<Parameter> {

        public Parameter mapRow(ResultSet rs, int rowNum) throws SQLException {
            Parameter parameter = new Parameter();
            parameter.setParameterCd(rs.getString("parameter_cd"));
            parameter.setParameterValue(rs.getString("parameter_value"));
            return parameter;
        }

    }
}
