package com.lhzkml.jasmine.core.config

import com.lhzkml.jasmine.core.agent.observe.event.EventCategory
import com.lhzkml.jasmine.core.agent.observe.snapshot.RollbackStrategy
import com.lhzkml.jasmine.core.agent.observe.trace.TraceEventCategory
import com.lhzkml.jasmine.core.agent.tools.ShellPolicy
import com.lhzkml.jasmine.core.prompt.llm.CompressionStrategyType

/**
 * In-memory implementation of [ConfigRepository] with sensible defaults.
 * Replace with SharedPreferences-backed implementation when persistence is needed.
 */
class InMemoryConfigRepository : ConfigRepository {

    // Provider management
    private var activeProviderId: String? = null
    private val apiKeys = mutableMapOf<String, String>()
    private val baseUrls = mutableMapOf<String, String>()
    private val models = mutableMapOf<String, String>()
    private val selectedModels = mutableMapOf<String, List<String>>()
    private val chatPaths = mutableMapOf<String, String>()
    private var customProviders = listOf<ProviderConfig>()

    override fun getActiveProviderId() = activeProviderId
    override fun setActiveProviderId(id: String) { activeProviderId = id }
    override fun getApiKey(providerId: String) = apiKeys[providerId]
    override fun saveProviderCredentials(providerId: String, apiKey: String, baseUrl: String?, model: String?) {
        apiKeys[providerId] = apiKey
        if (baseUrl != null) this.baseUrls[providerId] = baseUrl
        if (model != null) this.models[providerId] = model
    }
    override fun getBaseUrl(providerId: String) = baseUrls[providerId] ?: ""
    override fun getModel(providerId: String) = models[providerId] ?: ""
    override fun getSelectedModels(providerId: String) = selectedModels[providerId] ?: emptyList()
    override fun setSelectedModels(providerId: String, models: List<String>) { selectedModels[providerId] = models }
    override fun getChatPath(providerId: String) = chatPaths[providerId]
    override fun saveChatPath(providerId: String, path: String) { chatPaths[providerId] = path }

    // Vertex AI
    override fun isVertexAIEnabled(providerId: String) = false
    override fun setVertexAIEnabled(providerId: String, enabled: Boolean) {}
    override fun getVertexProjectId(providerId: String) = ""
    override fun setVertexProjectId(providerId: String, projectId: String) {}
    override fun getVertexLocation(providerId: String) = ""
    override fun setVertexLocation(providerId: String, location: String) {}
    override fun getVertexServiceAccountJson(providerId: String) = ""
    override fun setVertexServiceAccountJson(providerId: String, json: String) {}

    override fun loadCustomProviders() = customProviders
    override fun saveCustomProviders(providers: List<ProviderConfig>) { customProviders = providers }

    // LLM params
    private var systemPrompt = "You are a helpful assistant."
    private var maxTokens = 4096
    private var temperature = 0.7f
    private var topP = 0.9f
    private var topK = 40
    override fun getDefaultSystemPrompt() = systemPrompt
    override fun setDefaultSystemPrompt(prompt: String) { systemPrompt = prompt }
    override fun getMaxTokens() = maxTokens
    override fun setMaxTokens(maxTokens: Int) { this.maxTokens = maxTokens }
    override fun getTemperature() = temperature
    override fun setTemperature(value: Float) { temperature = value }
    override fun getTopP() = topP
    override fun setTopP(value: Float) { topP = value }
    override fun getTopK() = topK
    override fun setTopK(value: Int) { topK = value }

    // Timeouts
    private var requestTimeout = 120
    private var socketTimeout = 60
    private var connectTimeout = 30
    private var streamResumeEnabled = true
    private var streamResumeMaxRetries = 3
    override fun getRequestTimeout() = requestTimeout
    override fun setRequestTimeout(seconds: Int) { requestTimeout = seconds }
    override fun getSocketTimeout() = socketTimeout
    override fun setSocketTimeout(seconds: Int) { socketTimeout = seconds }
    override fun getConnectTimeout() = connectTimeout
    override fun setConnectTimeout(seconds: Int) { connectTimeout = seconds }
    override fun isStreamResumeEnabled() = streamResumeEnabled
    override fun setStreamResumeEnabled(enabled: Boolean) { streamResumeEnabled = enabled }
    override fun getStreamResumeMaxRetries() = streamResumeMaxRetries
    override fun setStreamResumeMaxRetries(value: Int) { streamResumeMaxRetries = value }

    // Tools
    private var toolsEnabled = true
    private var enabledTools = emptySet<String>()
    private var agentToolPreset = emptySet<String>()
    private var brightDataKey = ""
    override fun isToolsEnabled() = toolsEnabled
    override fun setToolsEnabled(enabled: Boolean) { toolsEnabled = enabled }
    override fun getEnabledTools() = enabledTools
    override fun setEnabledTools(tools: Set<String>) { enabledTools = tools }
    override fun getAgentToolPreset() = agentToolPreset
    override fun setAgentToolPreset(tools: Set<String>) { agentToolPreset = tools }
    override fun getBrightDataKey() = brightDataKey
    override fun setBrightDataKey(key: String) { brightDataKey = key }

    // Shell
    private var shellPolicy = ShellPolicy.MANUAL
    private var shellBlacklist = ShellPolicy.DEFAULT_BLACKLIST
    private var shellWhitelist = ShellPolicy.DEFAULT_WHITELIST
    override fun getShellPolicy() = shellPolicy
    override fun setShellPolicy(policy: ShellPolicy) { shellPolicy = policy }
    override fun getShellBlacklist() = shellBlacklist
    override fun setShellBlacklist(list: List<String>) { shellBlacklist = list }
    override fun getShellWhitelist() = shellWhitelist
    override fun setShellWhitelist(list: List<String>) { shellWhitelist = list }

    // MCP
    private var mcpEnabled = false
    private var mcpServers = listOf<McpServerConfig>()
    override fun isMcpEnabled() = mcpEnabled
    override fun setMcpEnabled(enabled: Boolean) { mcpEnabled = enabled }
    override fun getMcpServers() = mcpServers
    override fun setMcpServers(servers: List<McpServerConfig>) { mcpServers = servers }
    override fun addMcpServer(server: McpServerConfig) { mcpServers = mcpServers + server }
    override fun removeMcpServer(index: Int) { mcpServers = mcpServers.toMutableList().also { if (index in it.indices) it.removeAt(index) } }
    override fun updateMcpServer(index: Int, server: McpServerConfig) {
        mcpServers = mcpServers.toMutableList().also { if (index in it.indices) it[index] = server }
    }

    // Agent
    private var agentStrategy = AgentStrategyType.AUTO
    private var graphToolCallMode = GraphToolCallMode.AUTO
    private var toolSelectionStrategy = ToolSelectionStrategyType.AUTO
    private var toolSelectionNames = emptySet<String>()
    private var toolSelectionTaskDesc = ""
    private var toolChoiceMode = ToolChoiceMode.AUTO
    private var toolChoiceNamedTool = ""
    private var agentMaxIterations = 15
    private var maxToolResultLength = 4000
    override fun getAgentStrategy() = agentStrategy
    override fun setAgentStrategy(strategy: AgentStrategyType) { agentStrategy = strategy }
    override fun getGraphToolCallMode() = graphToolCallMode
    override fun setGraphToolCallMode(mode: GraphToolCallMode) { graphToolCallMode = mode }
    override fun getToolSelectionStrategy() = toolSelectionStrategy
    override fun setToolSelectionStrategy(strategy: ToolSelectionStrategyType) { toolSelectionStrategy = strategy }
    override fun getToolSelectionNames() = toolSelectionNames
    override fun setToolSelectionNames(names: Set<String>) { toolSelectionNames = names }
    override fun getToolSelectionTaskDesc() = toolSelectionTaskDesc
    override fun setToolSelectionTaskDesc(desc: String) { toolSelectionTaskDesc = desc }
    override fun getToolChoiceMode() = toolChoiceMode
    override fun setToolChoiceMode(mode: ToolChoiceMode) { toolChoiceMode = mode }
    override fun getToolChoiceNamedTool() = toolChoiceNamedTool
    override fun setToolChoiceNamedTool(name: String) { toolChoiceNamedTool = name }
    override fun getAgentMaxIterations() = agentMaxIterations
    override fun setAgentMaxIterations(value: Int) { agentMaxIterations = value }
    override fun getMaxToolResultLength() = maxToolResultLength
    override fun setMaxToolResultLength(value: Int) { maxToolResultLength = value }

    // Trace
    private var traceEnabled = false
    private var traceFileEnabled = false
    private var traceFilter = setOf<TraceEventCategory>()
    override fun isTraceEnabled() = traceEnabled
    override fun setTraceEnabled(enabled: Boolean) { traceEnabled = enabled }
    override fun isTraceFileEnabled() = traceFileEnabled
    override fun setTraceFileEnabled(enabled: Boolean) { traceFileEnabled = enabled }
    override fun getTraceEventFilter() = traceFilter
    override fun setTraceEventFilter(categories: Set<TraceEventCategory>) { traceFilter = categories }

    // Planner
    private var plannerEnabled = true
    private var plannerMaxIterations = 5
    private var plannerCriticEnabled = false
    override fun isPlannerEnabled() = plannerEnabled
    override fun setPlannerEnabled(enabled: Boolean) { plannerEnabled = enabled }
    override fun getPlannerMaxIterations() = plannerMaxIterations
    override fun setPlannerMaxIterations(value: Int) { plannerMaxIterations = value }
    override fun isPlannerCriticEnabled() = plannerCriticEnabled
    override fun setPlannerCriticEnabled(enabled: Boolean) { plannerCriticEnabled = enabled }

    // Snapshot
    private var snapshotEnabled = false
    private var snapshotStorage = SnapshotStorageType.MEMORY
    private var snapshotAutoCheckpoint = false
    private var rollbackStrategy = RollbackStrategy.MANUAL
    override fun isSnapshotEnabled() = snapshotEnabled
    override fun setSnapshotEnabled(enabled: Boolean) { snapshotEnabled = enabled }
    override fun getSnapshotStorage() = snapshotStorage
    override fun setSnapshotStorage(storage: SnapshotStorageType) { snapshotStorage = storage }
    override fun isSnapshotAutoCheckpoint() = snapshotAutoCheckpoint
    override fun setSnapshotAutoCheckpoint(enabled: Boolean) { snapshotAutoCheckpoint = enabled }
    override fun getSnapshotRollbackStrategy() = rollbackStrategy
    override fun setSnapshotRollbackStrategy(strategy: RollbackStrategy) { rollbackStrategy = strategy }

    // Event handler
    private var eventHandlerEnabled = false
    private var eventFilter = setOf<EventCategory>()
    override fun isEventHandlerEnabled() = eventHandlerEnabled
    override fun setEventHandlerEnabled(enabled: Boolean) { eventHandlerEnabled = enabled }
    override fun getEventHandlerFilter() = eventFilter
    override fun setEventHandlerFilter(categories: Set<EventCategory>) { eventFilter = categories }

    // Compression
    private var compressionEnabled = true
    private var compressionStrategy = CompressionStrategyType.AUTO
    private var compressionMaxTokens = 8000
    private var compressionThreshold = 6000
    private var compressionLastN = 20
    private var compressionChunkSize = 4000
    private var compressionKeepRecentRounds = 3
    override fun isCompressionEnabled() = compressionEnabled
    override fun setCompressionEnabled(enabled: Boolean) { compressionEnabled = enabled }
    override fun getCompressionStrategy() = compressionStrategy
    override fun setCompressionStrategy(strategy: CompressionStrategyType) { compressionStrategy = strategy }
    override fun getCompressionMaxTokens() = compressionMaxTokens
    override fun setCompressionMaxTokens(value: Int) { compressionMaxTokens = value }
    override fun getCompressionThreshold() = compressionThreshold
    override fun setCompressionThreshold(value: Int) { compressionThreshold = value }
    override fun getCompressionLastN() = compressionLastN
    override fun setCompressionLastN(value: Int) { compressionLastN = value }
    override fun getCompressionChunkSize() = compressionChunkSize
    override fun setCompressionChunkSize(value: Int) { compressionChunkSize = value }
    override fun getCompressionKeepRecentRounds() = compressionKeepRecentRounds
    override fun setCompressionKeepRecentRounds(value: Int) { compressionKeepRecentRounds = value }

    // Rules
    private var personalRules = ""
    private val projectRules = mutableMapOf<String, String>()
    override fun getPersonalRules() = personalRules
    override fun setPersonalRules(rules: String) { personalRules = rules }
    override fun getProjectRules(workspacePath: String) = projectRules[workspacePath] ?: ""
    override fun setProjectRules(workspacePath: String, rules: String) { projectRules[workspacePath] = rules }

    // RAG
    private var ragEnabled = false
    private var ragTopK = 5
    private var ragEmbeddingBaseUrl = ""
    private var ragEmbeddingApiKey = ""
    private var ragEmbeddingModel = ""
    private var ragEmbeddingUseLocal = false
    private var ragEmbeddingModelPath = ""
    private var ragLibraries = listOf<RagLibraryConfig>()
    private var ragActiveLibraryIds = emptySet<String>()
    private var ragIndexableExtensions = setOf("kt", "java", "py", "md", "json", "xml", "yaml")
    override fun isRagEnabled() = ragEnabled
    override fun setRagEnabled(enabled: Boolean) { ragEnabled = enabled }
    override fun getRagTopK() = ragTopK
    override fun setRagTopK(value: Int) { ragTopK = value }
    override fun getRagEmbeddingBaseUrl() = ragEmbeddingBaseUrl
    override fun setRagEmbeddingBaseUrl(url: String) { ragEmbeddingBaseUrl = url }
    override fun getRagEmbeddingApiKey() = ragEmbeddingApiKey
    override fun setRagEmbeddingApiKey(key: String) { ragEmbeddingApiKey = key }
    override fun getRagEmbeddingModel() = ragEmbeddingModel
    override fun setRagEmbeddingModel(model: String) { ragEmbeddingModel = model }
    override fun getRagEmbeddingUseLocal() = ragEmbeddingUseLocal
    override fun setRagEmbeddingUseLocal(useLocal: Boolean) { ragEmbeddingUseLocal = useLocal }
    override fun getRagEmbeddingModelPath() = ragEmbeddingModelPath
    override fun setRagEmbeddingModelPath(path: String) { ragEmbeddingModelPath = path }
    override fun getRagLibraries() = ragLibraries
    override fun setRagLibraries(libraries: List<RagLibraryConfig>) { ragLibraries = libraries }
    override fun getRagActiveLibraryIds() = ragActiveLibraryIds
    override fun setRagActiveLibraryIds(ids: Set<String>) { ragActiveLibraryIds = ids }
    override fun getRagIndexableExtensions() = ragIndexableExtensions
    override fun setRagIndexableExtensions(extensions: Set<String>) { ragIndexableExtensions = extensions }

    // Agent mode
    private var agentMode = false
    private var workspacePath = ""
    private var workspaceUri = ""
    private var lastConversationId = ""
    private var lastSession = false
    override fun isAgentMode() = agentMode
    override fun setAgentMode(enabled: Boolean) { agentMode = enabled }
    override fun getWorkspacePath() = workspacePath
    override fun setWorkspacePath(path: String) { workspacePath = path }
    override fun getWorkspaceUri() = workspaceUri
    override fun setWorkspaceUri(uri: String) { workspaceUri = uri }
    override fun getLastConversationId() = lastConversationId
    override fun setLastConversationId(id: String) { lastConversationId = id }
    override fun hasLastSession() = lastSession
    override fun setLastSession(active: Boolean) { lastSession = active }
}
