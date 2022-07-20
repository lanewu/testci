package py.db;

public class TableQuery {
  private StringBuilder selectedItems = new StringBuilder();
  private StringBuilder table = new StringBuilder();
  private StringBuilder condition = new StringBuilder();
  private StringBuilder order = new StringBuilder();

  public  TableQuery appendToSelectedItems(String a) {
    selectedItems.append(a);
    return this;
  }
  
  public  TableQuery appendToTable(String a) {
    table.append(a);
    return this;
  }
  
  public TableQuery appendToCondition(String a) {
    condition.append(a);
    return this;
  }
  
  public TableQuery appendToOrder(String a) {
    order.append(a);
    return this;
  }
  
  public String getTable() {
    return table.toString();
  }
  
  public String getCondition() {
    return condition.toString();
  }
  
  public String getOrder() {
    return order.toString();
  }
  
  public String getSelectedItems() {
    return selectedItems.toString();
  }
  
  // If table is null or empty, the query is not valid
  public boolean isValid() {
    if (table.toString().trim().length() == 0) {
      return false;
    } else {
      return true;
    }
  }
}