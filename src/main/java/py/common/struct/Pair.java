package py.common.struct;

public class Pair<A, B> extends Object{
  private A first;
  private B second;

  public Pair()
  {
    this.first = null;
    this.second = null;
  }

  public void setFirst(A first)
  {
    this.first = first;
  }

  public void setSecond(B second)
  {
    this.second = second;
  }


  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  public A getFirst() { return first; }
  public B getSecond() { return second; }

  @Override
public String toString() {
    return "(" + first + ", " + second + ")";
  }

  private boolean equals(Object x, Object y) {
    return (x == null && y == null) || (x != null && x.equals(y));
  }

  @Override
public boolean equals(Object other) {
    return
      other instanceof Pair &&
      equals(first, ((Pair)other).first) &&
      equals(second, ((Pair)other).second);
  }

  @Override
public int hashCode() {
    if (first == null) return (second == null) ? 0 : second.hashCode() + 1;
    else if (second == null) return first.hashCode() + 2;
    else return first.hashCode() * 17 + second.hashCode();
  }
}
