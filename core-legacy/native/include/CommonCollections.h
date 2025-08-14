#ifndef _OSMAND_COMMON_COLLECTIONS_H_
#define _OSMAND_COMMON_COLLECTIONS_H_

// Unordered containers
#if defined(ANDROID) || defined(__ANDROID__)
#   include <unordered_map>
#   include <unordered_set>
#   define UNORDERED_NAMESPACE std
#   define UNORDERED_map unordered_map
#   define UNORDERED_set unordered_set
#elif defined(__linux__)
#   include <unordered_map>
#   include <unordered_set>
#   define UNORDERED_NAMESPACE std
#   define UNORDERED_map unordered_map
#   define UNORDERED_set unordered_set
#elif defined(__APPLE__)
#   include <unordered_map>
#   include <unordered_set>
#   define UNORDERED_NAMESPACE std
#   define UNORDERED_map unordered_map
#   define UNORDERED_set unordered_set
#elif defined(_WIN32) 
#   include <unordered_map>
#   include <unordered_set>
#   define UNORDERED_NAMESPACE std
#   define UNORDERED_map unordered_map
#   define UNORDERED_set unordered_set
#endif
#define UNORDERED(cls) UNORDERED_NAMESPACE::UNORDERED_##cls
 
// Smart pointers
#if defined(ANDROID) || defined(__ANDROID__)
#   include <memory>
#   define SHARED_PTR std::shared_ptr
#elif defined(__linux__)
#   include <memory>
#   define SHARED_PTR std::shared_ptr
#elif defined(__APPLE__)
#   include <memory>
#   define SHARED_PTR std::shared_ptr
#elif defined(_WIN32) 
#   include <memory>
#   define SHARED_PTR std::shared_ptr
#   define UNIQUE_PTR std::unique_ptr
#endif

#include <string>

typedef UNORDERED(map)<std::string, float> MAP_STR_FLOAT;
typedef UNORDERED(map)<std::string, std::string> MAP_STR_STR;

typedef unsigned int uint;
namespace OsmAnd
{
    typedef UNORDERED(map)<std::string, float> StringToFloatMap;
    typedef UNORDERED(map)<std::string, std::string> StringToStringMap;
}

#endif // _OSMAND_COMMON_COLLECTIONS_H_