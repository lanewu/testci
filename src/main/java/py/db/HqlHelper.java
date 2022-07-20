package py.db;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class HqlHelper {
  private static Logger log = Logger.getLogger(HqlHelper.class); 
  // building a condition. replace " and " with " where"
  private static String getCondition(String condition){
    StringBuilder conditionBuilder = new StringBuilder(" "); 
    if(!StringUtils.isBlank(condition)) {
      // Compile with case-insensitivity
      Pattern pattern = Pattern.compile("and", Pattern.CASE_INSENSITIVE);
      conditionBuilder.append(pattern.matcher(condition).replaceFirst("where"));
    }
    
    return conditionBuilder.toString();
  }
  
	public static String buildSelectHql(TableQuery query) {
    return buildSelectHql(query.getSelectedItems(), query.getTable(), query.getCondition(), query.getOrder());
	}
	
	public static String buildSelectHql(String items, String table, String condition, String order) {
    StringBuilder builder = new StringBuilder();
    
    builder.append(StringUtils.isBlank(items) ? "" : "select " + items);
    builder.append(" from ").append(table);	
    builder.append(" ").append(getCondition(condition));
    if (!StringUtils.isBlank(order)) { 
      builder.append(" order by ").append(order);
    }
    
    log.debug(builder.toString());
    return builder.toString();
  }
	 
  public static String buildCountHql(TableQuery query) {
    return buildCountHql(query.getTable(), query.getCondition());
  }
  
	public static String buildCountHql(String table, String condition) {
    StringBuilder builder = new StringBuilder("select count(*) from ");
    
    builder.append(table);
    builder.append(" ").append(getCondition(condition));
    log.debug(builder.toString());
    return builder.toString();
  }  
	
	public static String buildDeleteHql(TableQuery query) {
    return buildCountHql(query.getTable(), query.getCondition());
  }
  
  public static String buildDeleteHql(String table, String condition) {
    StringBuilder builder = new StringBuilder("delete from ");
    
    builder.append(table);
    builder.append(" ").append(getCondition(condition));
    log.debug(builder.toString());
    return builder.toString();
  }
}