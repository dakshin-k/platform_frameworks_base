/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "androidfw/ResourceUtils.h"

namespace android {

bool ExtractResourceName(const StringPiece& str, StringPiece* out_package, StringPiece* out_type,
                         StringPiece* out_entry) {
  *out_package = "";
  *out_type = "";
  bool has_package_separator = false;
  bool has_type_separator = false;
  const char* start = str.data();
  const char* end = start + str.size();
  if (start[0] == '@') {
      start++;
  }
  const char* current = start;
  while (current != end) {
    if (out_type->size() == 0 && *current == '/') {
      has_type_separator = true;
      out_type->assign(start, current - start);
      start = current + 1;
    } else if (out_package->size() == 0 && *current == ':') {
      has_package_separator = true;
      out_package->assign(start, current - start);
      start = current + 1;
    }
    current++;
  }
  out_entry->assign(start, end - start);

  return !(has_package_separator && out_package->empty()) &&
         !(has_type_separator && out_type->empty());
}

base::expected<AssetManager2::ResourceName, NullOrIOError> ToResourceName(
    const StringPoolRef& type_string_ref, const StringPoolRef& entry_string_ref,
    const StringPiece& package_name) {
  AssetManager2::ResourceName name{
    .package = package_name.data(),
    .package_len = package_name.size(),
  };

  if (base::expected<StringPiece, NullOrIOError> type_str = type_string_ref.string8()) {
    name.type = type_str->data();
    name.type_len = type_str->size();
  } else if (UNLIKELY(IsIOError(type_str))) {
    return base::unexpected(type_str.error());
  }

  if (name.type == nullptr) {
    if (base::expected<StringPiece16, NullOrIOError> type16_str = type_string_ref.string16()) {
      name.type16 = type16_str->data();
      name.type_len = type16_str->size();
    } else if (!type16_str.has_value()) {
      return base::unexpected(type16_str.error());
    }
  }

  if (base::expected<StringPiece, NullOrIOError> entry_str = entry_string_ref.string8()) {
    name.entry = entry_str->data();
    name.entry_len = entry_str->size();
  } else if (UNLIKELY(IsIOError(entry_str))) {
    return base::unexpected(entry_str.error());
  }

  if (name.entry == nullptr) {
    if (base::expected<StringPiece16, NullOrIOError> entry16_str = entry_string_ref.string16()) {
      name.entry16 = entry16_str->data();
      name.entry_len = entry16_str->size();
    } else if (!entry16_str.has_value()) {
      return base::unexpected(entry16_str.error());
    }
  }

  return name;
}

std::string ToFormattedResourceString(const AssetManager2::ResourceName& resource_name) {
  std::string result;
  if (resource_name.package != nullptr) {
    result.append(resource_name.package, resource_name.package_len);
  }

  if (resource_name.type != nullptr || resource_name.type16 != nullptr) {
    if (!result.empty()) {
      result += ":";
    }

    if (resource_name.type != nullptr) {
      result.append(resource_name.type, resource_name.type_len);
    } else {
      result += util::Utf16ToUtf8(StringPiece16(resource_name.type16, resource_name.type_len));
    }
  }

  if (resource_name.entry != nullptr || resource_name.entry16 != nullptr) {
    if (!result.empty()) {
      result += "/";
    }

    if (resource_name.entry != nullptr) {
      result.append(resource_name.entry, resource_name.entry_len);
    } else {
      result += util::Utf16ToUtf8(StringPiece16(resource_name.entry16, resource_name.entry_len));
    }
  }

  return result;
}

}  // namespace android
