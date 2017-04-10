package neo4j.movies.services

import grails.gorm.transactions.ReadOnly
import grails.neo4j.services.Cypher
import grails.gorm.services.Join

//tag::service[]
import grails.gorm.services.Service
import neo4j.movies.domain.Movie
import neo4j.movies.domain.Person

@Service(Movie)
abstract class MovieService {
//end::service[]
    /**
     * Example of where queries with joins to obtain a relationship
     */
    //tag::find[]
    @Join('cast')
    abstract Movie find(String title)
    //end::find[]

    /**
     * Example of where queries with regular expression for search with Neo4j
     **/
    //tag::search[]
    @ReadOnly
    List<Movie> search(String q, int limit = 100) { // <1>
        List<Movie> results
        if(q) {
            results = Movie.where {
                title ==~ "%${q}%"  // <2>
            }.list(max:limit)
        }
        else {
            results = [] // <3>
        }
        return results
    }
    //end::search[]

    /**
     * Example of executing native Cypher queries
     */
    //tag::graph[]
    @Cypher("""MATCH ${Movie m}<-[:ACTED_IN]-${Person p}
               RETURN ${m.title} as movie, collect(${p.name}) as cast 
               LIMIT $limit""")
    protected abstract List<Map<String, Iterable<String>>> findMovieTitlesAndCast(int limit)
    //end::graph[]

    //tag::d3format[]
    @ReadOnly
    Map<String, Object> graph(int limit = 100) {
        return toD3Format(findMovieTitlesAndCast(limit))
    }

    private Map<String, Object> toD3Format(List<Map<String, Iterable<String>>> result) {
        List<Map<String,String>> nodes = []
        List<Map<String,Object>> rels= []
        int i=0
        for(entry in result) {
            nodes << [title: entry.movie, label:"movie"]
            int target=i
            i++
            for (String name : (Iterable<String>) entry.cast) {
                def actor = [title: name, label:"actor"]
                int source = nodes.indexOf(actor)
                if (source == -1) {
                    nodes << actor
                    source = i++
                }
                rels << [source:source, target: target]
            }
        }
        return [nodes: nodes, links: rels]
    }
    //end::d3format[]
}
