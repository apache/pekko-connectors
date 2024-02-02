package docs.javadsl;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.auth.PasswordAuthenticator;

public class Example {

  public static void main(String[] args) {
    CouchbaseCluster cluster = CouchbaseCluster.create("localhost");
    cluster.authenticate(new PasswordAuthenticator("Administrator", "password"));
    Bucket bucket = cluster.openBucket("akka");
  }

}
