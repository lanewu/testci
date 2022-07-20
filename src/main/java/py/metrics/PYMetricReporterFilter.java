package py.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import java.util.regex.Pattern;

public enum PYMetricReporterFilter implements MetricFilter {

  Graphite, CSV, Ganglia;

  private String regex;
  private Pattern pattern;

  public void compilePattern(String regex) {
    this.regex = regex;
    this.pattern = Pattern.compile(regex);
  }

  public String getRegex() {
    return regex;
  }

  @Override
  public boolean matches(String name, Metric metric) {

    if (pattern == null) {
      return false;
    }

    return pattern.matcher(name).find();
  }

}
