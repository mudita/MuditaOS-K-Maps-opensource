package com.mudita.download.repository.models

import java.text.Collator

sealed interface Resource {
    val id: String

    companion object {
        val collator: Collator = Collator.getInstance()

        val NameComparator = Comparator<Resource> { r1, r2 ->
            collator.compare(r1.getName(), r2.getName())
        }

        private fun Resource.getName(): String =
            when (this) {
                is DownloadItem -> name
                is DownloadRegions -> "" // DownloadRegions must be 1st in the list, and it's manually extracted from the list in the UI.
                is ResourceGroup -> name
            }
    }
}
