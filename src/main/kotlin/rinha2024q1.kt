import com.fasterxml.jackson.annotation.JsonProperty
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.logging.Log
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.*
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import java.time.LocalDateTime

@Entity
@Table(name = "clientes")
class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var nome: String? = null
    var saldo: Long? = null
    var limite: Long? = null

    @Version
    var version: Long? = null

    fun getTotal(): Long {
        return saldo!! + limite!!
    }
}

@Entity
@Table(name = "transacoes")
class Transacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var valor: Long? = null
    var tipo: Char? = null
    var descricao: String? = null

    @Column(name = "cliente_id")
    var clienteId: Long? = null

    @Column(name = "criado")
    var createAt: LocalDateTime? = null
}

@ApplicationScoped
class ClienteRepository : PanacheRepository<Cliente>

@ApplicationScoped
class TransacaoRepository : PanacheRepository<Transacao> {
    fun findByCliente(clienteId: Long): List<Transacao> {
        return find("clienteId", clienteId).list()
    }

    fun ultimas10Transacoes(clienteId: Long): List<Transacao> {
        val jpql = "select t from  Transacao t  where t.clienteId = :clienteId order by t.id desc"
        val query = getEntityManager().createQuery(jpql, Transacao::class.java)
        query.setParameter("clienteId", clienteId)
        query.setFirstResult(0)
        query.setMaxResults(10)
        return query.resultList
    }
}

@ApplicationScoped
class TransacoesService(
    val clienteRepository: ClienteRepository,
    val transacaoRepository: TransacaoRepository,
) {

    private fun getCliente(id: Long): Cliente {
        return clienteRepository.findById(id) ?: throw NotFoundException()
    }

    fun extrato(clienteId: Long): ExtratoResponse {
        val cliente = getCliente(clienteId)
        val ultimasTransacoes = transacaoRepository
            .ultimas10Transacoes(clienteId)
            .map {
                TransacaoResponse(
                    valor = it.valor!!,
                    tipo = it.tipo!!,
                    descricao = it.descricao!!,
                    realizadaEm = it.createAt!!,
                )
            }
        return ExtratoResponse(
            saldo = SaldoResponse(
                total = cliente.saldo!!,
                limite = cliente.limite!!,
                dataExtrato = LocalDateTime.now(),
            ),
            ultimasTransacoes = ultimasTransacoes,
        )
    }

    fun criarTransacao(clienteId: Long, payload: CreateTransacaoPayload): CreateTransacaoResponse {
        var cliente: Cliente? = null

        while (cliente == null) {
            try {
                cliente = tryUpdate(clienteId, payload)
            } catch (ex: OptimisticLockException) {
                //try again
            }
        }

        return CreateTransacaoResponse(
            saldo = cliente.saldo!!,
            limite = cliente.limite!!,
        )
    }

    @Transactional
    @WithSpan
    protected fun tryUpdate(clienteId: Long, payload: CreateTransacaoPayload): Cliente {
        val cliente = getCliente(clienteId)

        if (payload.tipo == 'd' && cliente.getTotal() < payload.valorLong) {
            throw RegraException(null)
        }

        if (payload.tipo == 'd') {
            cliente.saldo = cliente.saldo!! - payload.valorLong
        } else {
            cliente.saldo = cliente.saldo!! + payload.valorLong
        }

        clienteRepository.persistAndFlush(cliente)

        val transacao = Transacao()
        transacao.clienteId = cliente.id
        transacao.createAt = LocalDateTime.now()
        transacao.valor = payload.valorLong
        transacao.tipo = payload.tipo
        transacao.descricao = payload.descricao
        transacaoRepository.persistAndFlush(transacao)
        return cliente
    }

}

//POST /clientes/[id]/transacoes

@ApplicationScoped
@Path("clientes")
class ClienteResource(
    val service: TransacoesService
) {

    @Path("{id}/transacoes")
    @POST
    fun createTransacao(
        @PathParam("id") clienteId: Long,
        payload: CreateTransacaoPayload,
    ): CreateTransacaoResponse {
        Log.infov("create-transaction client: $clienteId value: ${payload.valor}")
        validatePayload(payload)
        return service.criarTransacao(clienteId, payload)
    }

    @GET
    @Path("{id}/extrato")
    fun extrato(
        @PathParam("id") clienteId: Long,
    ): ExtratoResponse {
        return service.extrato(clienteId)
    }

    private fun validatePayload(payload: CreateTransacaoPayload) {
        if (payload.valorLong <= 0) {
            throw RegraException("valor deve ser positivo")
        }
        if (payload.descricao.isEmpty()) {
            throw RegraException("campo descrição é obrigatório")
        }
        if (payload.descricao.length > 10) {
            throw RegraException("campo descrição não pode ter tamanho maior que 10")
        }
        if (!listOf('c', 'd').contains(payload.tipo)) {
            throw RegraException("tipo ${payload.tipo} não é valido")
        }
    }
}

class RegraException(message: String?) : RuntimeException(message)

@Provider
class RegraExceptionMapper : ExceptionMapper<RegraException> {
    override fun toResponse(exception: RegraException?): Response {
        return Response.status(422).build()
    }
}

@RegisterForReflection
data class CreateTransacaoPayload(
    val valor: String,
    val tipo: Char,
    val descricao: String,
) {
    val valorLong: Long
    init {
        valorLong = convertValor()
    }
    private fun convertValor(): Long {
        try {
            return valor.toLong()
        } catch (e: NumberFormatException) {
            throw RegraException("valor invalido!")
        }
    }
}

@RegisterForReflection
data class CreateTransacaoResponse(
    val limite: Long,
    val saldo: Long,
)


@RegisterForReflection
data class ExtratoResponse(
    val saldo: SaldoResponse,
    @JsonProperty("ultimas_transacoes")
    val ultimasTransacoes: List<TransacaoResponse>,
)

@RegisterForReflection
data class SaldoResponse(
    val total: Long,
    @JsonProperty("data_extrato")
    val dataExtrato: LocalDateTime,
    val limite: Long,
)

@RegisterForReflection
data class TransacaoResponse(
    val valor: Long,
    val tipo: Char,
    val descricao: String,
    @JsonProperty("realizada_em")
    val realizadaEm: LocalDateTime
)
