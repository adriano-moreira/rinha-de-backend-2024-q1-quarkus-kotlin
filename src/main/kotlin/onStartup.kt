import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import io.quarkus.runtime.configuration.ConfigUtils
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.transaction.Transactional

@ApplicationScoped
class OnStartup(
    val clienteRepository: ClienteRepository
) {

    fun onStartup(@Observes event: StartupEvent) {
        val profiles = ConfigUtils.getProfiles()
        Log.infov("profiles: {0},", profiles)
        if (profiles.contains("dev") || profiles.contains("test")) {
            addCliente(100000)
            addCliente(80000)
            addCliente(1000000)
            addCliente(10000000)
            addCliente(500000)
        }
    }

    var count = 0

    @Transactional
    fun addCliente(limite: Long) {
        val c = Cliente()
        c.nome = "Cliente ${++count}"
        c.limite = limite
        c.saldo = 0
        clienteRepository.persist(c)
    }

}
