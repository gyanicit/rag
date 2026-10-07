# Embedding Models Guide

This guide explains embedding models and the options available for this LangChain4j RAG application. It is intended as a reference for developers who maintain the ingestion and retrieval pipeline.

## What an embedding model does

An embedding model converts text into a numeric vector. Texts with similar meaning should have vectors that are close together, which lets a vector store such as pgvector find relevant document segments for a user's question.

Embedding models are separate from chat models. The embedding model is used when documents are indexed and when questions are searched. A chat model then uses the retrieved segments to compose an answer.

## Current project configuration

The application currently creates a Mistral embedding model in [`RagConfig.java`](src/main/java/com/icit/rag/config/RagConfig.java):

- Provider: Mistral AI
- Model: `mistral-embed`
- LangChain4j integration: `langchain4j-mistral-ai`
- Vector store: PostgreSQL with pgvector
- Tables: `baby_product_embedding` and `vaccine_embedding`

Both stores use `embeddingModel.dimension()` when they are created. This ensures the pgvector column dimension matches the configured model. The same `EmbeddingModel` bean is injected into the ingestion and retrieval services so both stages use compatible vectors.

The model API key is read from Spring configuration. Supply secrets through environment variables or a secret manager rather than committing them to source control.

## Common model options

LangChain4j uses a common `EmbeddingModel` interface, with provider-specific integration modules and builders. The following are representative options; the LangChain4j integration catalog changes over time.

Free-use information and portal URLs below were checked on **2026-10-08**. “Free” means a limited API allowance or free local software, not unlimited capacity or a free support contract. Provider plans, quotas, regional availability, and model eligibility can change; follow the linked pricing/key instructions before adopting a provider.

| Provider or runtime | Example model(s) | Maven artifact | Free tier / local use? | API key / login portal | When it may fit |
|---|---|---|---|---|---|
| Mistral AI | `mistral-embed` | `langchain4j-mistral-ai` | **Yes, limited.** Free mode has usage/rate limits and no card requirement to start; confirm the model is enabled for the account. ([Mistral key/free-mode instructions](https://docs.mistral.ai/getting-started/quickstarts/studio/activate-and-generate-api-key)) | [Mistral Console](https://console.mistral.ai/) → **API Keys** → **Create new key**. ([Step-by-step instructions](https://docs.mistral.ai/getting-started/quickstarts/studio/activate-and-generate-api-key)) | Current project option; hosted API. |
| OpenAI | `text-embedding-3-small`, `text-embedding-3-large` | `langchain4j-open-ai` | **No guaranteed free tier.** Free credits may be present on some accounts; otherwise API usage requires prepaid/paid billing. ([Billing details](https://help.openai.com/en/articles/8264644-how-can-i-set-up-prepaid-billing)) | [OpenAI API keys](https://platform.openai.com/api-keys) | Hosted API; the `text-embedding-3` models support configurable output dimensions. |
| Azure OpenAI | OpenAI embedding models deployed in Azure | `langchain4j-azure-open-ai` | **No guaranteed free model usage.** A free Azure subscription may be available, but the Azure OpenAI resource uses its configured pricing tier. ([Create resource and deploy a model](https://learn.microsoft.com/en-us/azure/cognitive-services/openai/how-to/create-resource?pivots=web-portal)) | [Azure Portal](https://portal.azure.com/) → open the Azure OpenAI resource → **Resource Management** → **Keys and Endpoint**. ([Microsoft setup instructions](https://learn.microsoft.com/en-us/azure/cognitive-services/openai/how-to/create-resource?pivots=web-portal)) | Useful when the application already uses Azure authentication, networking, or governance. |
| Google Gemini | `gemini-embedding-2`, `gemini-embedding-001` | `langchain4j-google-ai-gemini` | **Yes, for selected models and request modes.** Gemini Embedding 2 Standard currently lists free input; Batch is not available on the free tier. Check the current pricing table for the exact model. ([Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing)) | [Google AI Studio API keys](https://aistudio.google.com/app/apikey) ([Key setup instructions](https://ai.google.dev/gemini-api/docs/api-key)) | Hosted Google AI API; supports configurable output dimensionality and retrieval task types where the selected model/integration supports them. |
| Cohere | Embed models such as `embed-v4.0` | `langchain4j-cohere` | **Yes, limited.** New accounts receive a free, rate-limited trial key. ([Trial key and production details](https://docs.cohere.com/docs/going-live)) | [Cohere API Keys dashboard](https://dashboard.cohere.com/api-keys) | Hosted API; supports retrieval-oriented and multimodal use cases. |
| Voyage AI | Voyage embedding models | `langchain4j-voyage-ai` | **Yes, model-specific free token allowance.** The pricing page lists up to 200M free text tokens for specified current models; Batch API use does not consume this allowance. ([Voyage pricing](https://docs.voyageai.com/docs/pricing)) | [Voyage dashboard login](https://dashboard.voyageai.com/) → **API Keys** → create a secret key. ([Official key instructions](https://docs.voyageai.com/docs/api-key-and-installation)) | Hosted API; includes general-purpose, code, and multimodal model options. |
| Jina AI | Jina embedding models | `langchain4j-jina` | **Yes, limited.** New users receive 10M free tokens; free-tier rate limits apply. ([Jina API documentation and limits](https://api.jina.ai/docs)) | [Jina API key manager](https://jina.ai/api-dashboard/key-manager) | Hosted API; check the provider's current catalog for language and task coverage. |
| Hugging Face | Models such as `sentence-transformers/all-MiniLM-L6-v2` | `langchain4j-hugging-face` | **Yes, limited credits for routed Inference Providers.** Free accounts currently receive $0.10/month, subject to change; dedicated endpoints and usage beyond credits may be charged. ([Pricing and billing](https://huggingface.co/docs/inference-providers/en/pricing)) | [Hugging Face access tokens](https://huggingface.co/settings/tokens) → create a token with only the permissions needed. | Hosted inference; check the selected model's API support and the integration's version compatibility. |
| Ollama | `embeddinggemma`, `qwen3-embedding`, `all-minilm` | `langchain4j-ollama` | **Yes for local inference.** Ollama runs on infrastructure you operate; no hosted API usage charge for a local model. | **Not needed for local use.** Install/run Ollama locally; the application connects to its local server. | Local model server; keeps inference on infrastructure you operate. |
| In-process ONNX | All-MiniLM-L6-v2, BGE Small, E5 Small | A model-specific `langchain4j-embeddings-*` artifact | **Yes for local inference.** No paid embedding API is required; compute and model licensing still apply. | **Not needed** for a public model artifact. A gated Hugging Face download may require a [Hugging Face access token](https://huggingface.co/settings/tokens). | Runs locally in the Java process without an embedding API call. |
| Jlama | BERT-based Hugging Face models | `langchain4j-jlama` | **Yes for local inference.** No paid embedding API is required; compute and model licensing still apply. | **Not needed** for a public model. A gated Hugging Face model may require a [Hugging Face access token](https://huggingface.co/settings/tokens). | Local JVM inference for supported models. |

## Maven dependencies

Add the dependency for the provider/runtime you select. **Do not add every provider dependency**: the application creates one `EmbeddingModel` bean, so only the active model integration is needed.

This project already defines these version properties in `pom.xml`:

```xml
<langchain4j.version>1.21.0</langchain4j.version>
<langchain4j.integration.beta.version>1.21.0-beta31</langchain4j.integration.beta.version>
```

Use the stable version property for integrations published on the stable release line, and the beta property for integrations published on the beta line. LangChain4j modules do not all share the same release suffix; confirm the selected artifact's version in its integration guide when updating LangChain4j.

### Stable integration examples

These use `${langchain4j.version}`:

```xml
<!-- Keep this dependency for the current Mistral embedding model. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-mistral-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- OpenAI embedding models, for example text-embedding-3-small. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- Azure-hosted OpenAI embedding deployments. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-azure-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- Google AI Gemini embedding models. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-google-ai-gemini</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- Embedding models served by a local Ollama process. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

### Beta integration examples

These use `${langchain4j.integration.beta.version}`. Check the current provider guide if a particular module has a separate release line.

```xml
<!-- Cohere embedding models. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-cohere</artifactId>
    <version>${langchain4j.integration.beta.version}</version>
</dependency>

<!-- Voyage AI embedding models. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-voyage-ai</artifactId>
    <version>${langchain4j.integration.beta.version}</version>
</dependency>

<!-- Jina embedding models. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-jina</artifactId>
    <version>${langchain4j.integration.beta.version}</version>
</dependency>

<!-- Hugging Face hosted inference integration. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-hugging-face</artifactId>
    <version>${langchain4j.integration.beta.version}</version>
</dependency>

<!-- Select only one packaged ONNX model dependency. These artifacts run locally. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings-all-minilm-l6-v2</artifactId>
    <version>${langchain4j.integration.beta.version}</version>
</dependency>

<!-- Alternative: BGE Small English v1.5. Add instead of the All-MiniLM dependency. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings-bge-small-en-v15</artifactId>
    <version>${langchain4j.integration.beta.version}</version>
</dependency>

<!-- Alternative: E5 Small v2. Add instead of the All-MiniLM dependency. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings-e5-small-v2</artifactId>
    <version>${langchain4j.integration.beta.version}</version>
</dependency>

<!-- Local JVM inference with supported Hugging Face BERT-based models. -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-jlama</artifactId>
    <version>${langchain4j.integration.beta.version}</version>
</dependency>
```

The ONNX dependencies are model-specific: select one artifact matching the model class used in Java. For example, the All-MiniLM artifact pairs with `AllMiniLmL6V2EmbeddingModel`.

When switching providers, remove or replace the old model dependency if the application no longer uses it. Keep `langchain4j-core` and the provider integration versions compatible; avoid mixing arbitrary releases from different LangChain4j version lines.

Links to official integration documentation:

- [LangChain4j integration overview](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/intro.md)
- [Mistral AI](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/mistral-ai.md)
- [OpenAI](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/open-ai.md)
- [Azure OpenAI](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/azure-open-ai.md)
- [Google Gemini](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/google-ai-gemini.md)
- [Cohere](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/cohere.md)
- [Voyage AI](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/voyage-ai.md)
- [Hugging Face](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/hugging-face.md)
- [Ollama](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/ollama.md)
- [In-process ONNX models](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/1-in-process.md)
- [Jlama](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-models/jlama.md)

## Choosing a model

Compare candidate models using representative documents and questions from the application's actual knowledge bases. Consider:

1. **Language coverage:** Verify performance in every language used by the documents and users.
2. **Retrieval quality:** Check whether the right segments appear in the top results for real queries.
3. **Hosting and privacy:** Hosted APIs require network access and send text to the provider. Local models require you to operate the model runtime and provide enough compute.
4. **Latency and throughput:** Measure indexing speed and query latency at expected traffic levels.
5. **Cost and limits:** Review provider pricing, quotas, rate limits, and batch support.
6. **Dimension:** Confirm that the selected output dimension is supported by pgvector and matches the table schema.
7. **Query/document modes:** Some models can use different task types for indexing documents and embedding queries. Enable these only when supported, and configure both sides consistently.

## Switching the configured model

For example, using OpenAI requires adding its LangChain4j integration dependency and replacing the Mistral bean with an `OpenAiEmbeddingModel` bean. Keep the integration version aligned with the LangChain4j version used by this project (`1.21.0`).

```java
@Bean
public EmbeddingModel embeddingModel() {
    return OpenAiEmbeddingModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("text-embedding-3-small")
            .build();
}
```

The exact dependency, class, authentication, and builder settings differ by provider. The pgvector stores and the ingestion/retrieval code can continue to depend on the shared `EmbeddingModel` interface.

## Re-indexing is required when changing models

Vectors produced by different models are not interchangeable, even when they have the same number of dimensions. After changing the embedding model:

1. Configure the new model for both document ingestion and query retrieval.
2. Check the model's output dimension and confirm that each pgvector table uses that dimension.
3. Create replacement tables or otherwise safely migrate the tables. `createTable(true)` creates a missing table; it does not convert an existing table to another dimension.
4. Re-ingest every document so all stored vectors are produced by the new model.
5. Verify retrieval with representative queries before directing application traffic to the new index.

For production, build and verify replacement tables before switching traffic so the existing index remains available during re-indexing. Also retune retrieval settings such as `minScore` against the new model's results.

## Further reading

- [PostgreSQL, pgvector, and pgAdmin 4 setup guide](POSTGRES_PGADMIN_SETUP.md)
- [LangChain4j RAG guide](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/tutorials/rag.md)
- [pgvector embedding store configuration](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-stores/pgvector.md)
