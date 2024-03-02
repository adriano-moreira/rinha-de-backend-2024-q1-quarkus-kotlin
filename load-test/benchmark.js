// @ts-check

import { check } from "k6";
import { getExtrato, postTransacao } from "./lib.js";

// jvm      v1: http_reqs......................: 11385
// native   v1: http_reqs......................: 23210
export const options = {
    vus: 10,
    duration: '30s',
};

export default function () {
    const tipo = 'c'
    for (let clienteId = 1; clienteId <= 5; clienteId++) {
        for (let i = 1; i <= 10; i++) {
            const post = postTransacao(
                clienteId,
                {
                    valor: i,
                    tipo: tipo,
                    descricao: `desc ${i}`,
                },
            )
            check(post, { 'deve retornar 200': (post) => post.status === 200 })
        }
        const resp = getExtrato(clienteId);
        check(resp, { 'extrato deve retornar 200': (resp) => resp.status === 200 })
    }
}
