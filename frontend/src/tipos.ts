// Formatos que a API devolve (espelham os DTOs do backend).

export type Decisao = 'ACIMA' | 'PERTO' | 'ABAIXO' | 'FALTA_DADO'
export type Finalidade = 'GRAO' | 'SEMENTE'
export type Origem = 'FOTO' | 'AUDIO' | 'TEXTO'
export type StatusAlerta = 'PENDENTE' | 'CONFIRMADO' | 'REVISITAR'

export interface TalhaoResumo {
  id: number
  nome: string
  produtorId: number
  produtorNome: string
  hectares: number
  estadio: string
  finalidade: Finalidade
  latitude: number | null
  longitude: number | null
  decisao: Decisao | null
  ultimoMonitoramentoEm: string | null
  diasSemMonitorar: number
  mediaPercevejos: number | null
  mediaLagartas: number | null
  mediaDesfolha: number | null
  alertaId: number | null
}

export interface Amostra {
  id: number
  percevejos: number
  lagartas: number
  desfolha: number
  origem: Origem
  urlMidia: string | null
  transcricao: string | null
  criadaEm: string
}

export interface Monitoramento {
  id: number
  dataHora: string
  status: 'ABERTO' | 'CONCLUIDO' | 'CANCELADO'
  decisao: Decisao | null
  recomendacao: string | null
  mediaPercevejos: number | null
  mediaLagartas: number | null
  mediaDesfolha: number | null
  amostras: Amostra[]
}

export interface Aplicacao {
  id: number
  dataHora: string
  confirmada: boolean
}

export interface TalhaoDetalhe {
  talhao: TalhaoResumo
  monitoramentos: Monitoramento[]
  aplicacoes: Aplicacao[]
}

export interface Alerta {
  id: number
  status: StatusAlerta
  criadoEm: string
  resolvidoEm: string | null
  talhaoId: number
  talhaoNome: string
  produtorNome: string
  produtorTelefone: string
  hectares: number
  estadio: string
  decisao: Decisao | null
  mediaPercevejos: number | null
  mediaLagartas: number | null
  mediaDesfolha: number | null
}

export interface PressaoSemanal {
  semana: string
  rotulo: string
  totalPercevejos: number
  amostras: number
  mediaPorPano: number
}

export interface Dashboard {
  acima: TalhaoResumo[]
  perto: TalhaoResumo[]
  semMonitoramento: TalhaoResumo[]
  aplicacoesEvitadas: number
  aplicacoesCalendario: number
  aplicacoesFeitas: number
  economiaEstimada: number
  custoAplicacaoHa: number
  pressaoSemanal: PressaoSemanal[]
}

export interface Produtor {
  id: number
  nome: string
  telefone: string
  talhoes: TalhaoResumo[]
}

export interface ResumoConversa {
  produtorId: number
  nome: string
  telefone: string
  ultimaMensagem: string | null
  ultimaEm: string | null
}

export interface Opcao {
  id: string
  titulo: string
}

export interface Mensagem {
  id: number
  direcao: 'ENTRADA' | 'SAIDA'
  tipo: 'TEXTO' | 'IMAGEM' | 'AUDIO' | 'BOTOES' | 'LISTA' | 'TEMPLATE' | 'OUTRO'
  texto: string | null
  opcoes: Opcao[]
  urlMidia: string | null
  criadaEm: string
}

export interface NivelAcao {
  id: number
  chave: string
  descricao: string
  valor: number
  unidade: string
}

export interface StatusSistema {
  whatsapp: 'simulador' | 'cloud-api'
  claude: boolean
  whisper: boolean
  iaDemo: boolean
  custoAplicacaoHa: number
}
